/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.www.haystack.commons.secretDetector;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.netflix.servo.util.VisibleForTesting;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("WeakerAccess")
public class S3ConfigFetcher {
    @VisibleForTesting
    static final String ERROR_MESSAGE = "Exception getting white list items" +
            "; whitelisted finder/service/operation/tag combinations may not be correct";
    @VisibleForTesting
    static final AtomicReference<Map<String, Map<String, Map<String, Set<String>>>>> WHITE_LIST_ITEMS =
            new AtomicReference<>(new ConcurrentHashMap<>());
    @VisibleForTesting
    static final String INVALID_DATA_MSG = "The line [%s] does not contain at least three semicolons to separate "
            + "finderName, String serviceName, String operationName, String tagName";
    @VisibleForTesting
    static final String SUCCESSFUL_WHITELIST_UPDATE_MSG = "Successfully updated the whitelist from S3" +
            "; size=%d";

    private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);
    private final Logger logger;
    private final String bucket;
    private final String key;
    private final AmazonS3 amazonS3;
    private final Factory factory;
    @VisibleForTesting
    AtomicLong lastUpdateTime = new AtomicLong(0L);
    @VisibleForTesting
    AtomicBoolean isUpdateInProgress = new AtomicBoolean(false);

    public S3ConfigFetcher(Logger s3ConfigFetcherLogger,
                    WhiteListConfig whiteListConfig,
                    AmazonS3 amazonS3,
                    Factory s3ConfigFetcherFactory) {
        this.logger = s3ConfigFetcherLogger;
        this.bucket = whiteListConfig.bucket();
        this.key = whiteListConfig.key();
        this.amazonS3 = amazonS3;
        this.factory = s3ConfigFetcherFactory;
    }

    public Map<String, Map<String, Map<String, Set<String>>>> getWhiteListItems() {
        final long now = factory.createCurrentTimeMillis();
        if (now - lastUpdateTime.get() > ONE_HOUR) {
            if (isUpdateInProgress.compareAndSet(false, true)) {
                try {
                    WHITE_LIST_ITEMS.set(readAllWhiteListItemsFromS3());
                    lastUpdateTime.set(now);
                    logger.info(String.format(SUCCESSFUL_WHITELIST_UPDATE_MSG, getWhiteListItemSize()));
                } catch (InvalidWhitelistItemInputException e) {
                    logger.error(e.getMessage(), e);
                } catch (Exception e) {
                    logger.error(ERROR_MESSAGE, e);
                } finally {
                    isUpdateInProgress.set(false);
                }
            }
        }
        return WHITE_LIST_ITEMS.get();
    }

    private int getWhiteListItemSize() {
        int size = 0;
        final Collection<Map<String, Map<String, Set<String>>>> values = WHITE_LIST_ITEMS.get().values();
        for (final Map<String, Map<String, Set<String>>> stringMapMap : values) {
            for (final Map<String, Set<String>> stringSetMap : stringMapMap.values()) {
                for (final Set<String> strings : stringSetMap.values()) {
                    size += strings.size();
                }
            }
        }
        return size;
    }

    private Map<String, Map<String, Map<String, Set<String>>>> readAllWhiteListItemsFromS3()
            throws IOException, InvalidWhitelistItemInputException {
        try (final S3Object s3Object = amazonS3.getObject(bucket, key)) {
            final BufferedReader bufferedReader = getBufferedReader(s3Object);
            final Map<String, Map<String, Map<String, Set<String>>>> whiteListItems = new ConcurrentHashMap<>();
            WhiteListItem whiteListItem = readSingleWhiteListItemFromS3(bufferedReader);
            while (whiteListItem != null) {
                putTagInWhiteListItems(whiteListItems, whiteListItem);
                whiteListItem = readSingleWhiteListItemFromS3(bufferedReader);
            }
            return whiteListItems;
        }
    }

    private void putTagInWhiteListItems(Map<String, Map<String, Map<String, Set<String>>>> whiteListItems,
                                        WhiteListItem whiteListItem) {
        final Map<String, Map<String, Set<String>>> finderNameMap =
                whiteListItems.computeIfAbsent(whiteListItem.finderName, v -> new ConcurrentHashMap<>());
        final Map<String, Set<String>> serviceNameMap =
                finderNameMap.computeIfAbsent(whiteListItem.serviceName, v -> new ConcurrentHashMap<>());
        final Set<String> tags =
                serviceNameMap.computeIfAbsent(whiteListItem.operationName, v -> ConcurrentHashMap.newKeySet());
        tags.add(whiteListItem.tagName);
    }

    private BufferedReader getBufferedReader(S3Object s3Object) {
        final InputStream inputStream = s3Object.getObjectContent();
        final InputStreamReader inputStreamReader = factory.createInputStreamReader(inputStream);
        return factory.createBufferedReader(inputStreamReader);
    }

    public boolean isTagInWhiteList(String finderName, String serviceName, String operationName, String tagName) {
        final Map<String, Map<String, Map<String, Set<String>>>> finderNameMap = getWhiteListItems();
        final Map<String, Map<String, Set<String>>> serviceNameMap = finderNameMap.get(finderName);
        if (serviceNameMap != null) {
            final Map<String, Set<String>> operationNameMap = serviceNameMap.get(serviceName);
            if (operationNameMap != null) {
                final Set<String> tagNameSet = operationNameMap.get(operationName);
                if (tagNameSet != null) {
                    return tagNameSet.contains(tagName);
                }
            }
        }
        return false;
    }

    /**
     * Reads a line from S3 and transforms it to a WhiteListItem
     *
     * @param reader the reader
     * @return a non-null WhiteListItem if the read was successful, else null (which indicates all lines have been read)
     * @throws IOException                        if a problem occurs reading from S3
     * @throws InvalidWhitelistItemInputException if an input line in the S3 file is not formatted properly
     */
    private WhiteListItem readSingleWhiteListItemFromS3(BufferedReader reader)
            throws IOException, InvalidWhitelistItemInputException {
        final String line = reader.readLine();
        if (line == null) {
            return null;
        }
        final String[] strings = line.split(";");
        if (strings.length >= 4) {
            return new WhiteListItem(strings[0], strings[1], strings[2], strings[3]);
        }
        throw new InvalidWhitelistItemInputException(line);
    }

    public static class InvalidWhitelistItemInputException extends Exception {
        InvalidWhitelistItemInputException(String line) {
            super(String.format(INVALID_DATA_MSG, line));
        }
    }

    public static class Factory {
        long createCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        InputStreamReader createInputStreamReader(InputStream inputStream) {
            return new InputStreamReader(inputStream);
        }

        BufferedReader createBufferedReader(InputStreamReader inputStreamReader) {
            return new BufferedReader(inputStreamReader);
        }
    }
}
