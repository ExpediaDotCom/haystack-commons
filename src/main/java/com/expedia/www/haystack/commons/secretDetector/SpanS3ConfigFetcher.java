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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.netflix.servo.util.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("WeakerAccess")
public class SpanS3ConfigFetcher extends S3ConfigFetcherBase {
    private static final int ITEM_COUNT = 4;
    @VisibleForTesting
    static final String ERROR_MESSAGE = "Exception getting white list items" +
            "; whitelisted finder/service/operation/tag combinations may not be correct";
    @VisibleForTesting
    final AtomicReference<Map<String, Map<String, Map<String, Set<String>>>>> whiteListItems =
            new AtomicReference<>(new ConcurrentHashMap<>());
    @VisibleForTesting
    static final String INVALID_DATA_MSG = "The line [%s] does not contain at least three semicolons to separate "
            + "finderName, String serviceName, String operationName, String tagName";
    @VisibleForTesting
    static final String SUCCESSFUL_WHITELIST_UPDATE_MSG = "Successfully updated the whitelist from S3";

    private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);
    @VisibleForTesting
    AtomicLong lastUpdateTime = new AtomicLong(0L);
    @VisibleForTesting
    AtomicBoolean isUpdateInProgress = new AtomicBoolean(false);

    public SpanS3ConfigFetcher(String bucket, String key) {
        super(LoggerFactory.getLogger(SpanS3ConfigFetcher.class), bucket, key,
                AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2).build(), new SpanFactory(), ITEM_COUNT);
    }

    public SpanS3ConfigFetcher(Logger s3ConfigFetcherLogger,
                               WhiteListConfig whiteListConfig,
                               AmazonS3 amazonS3,
                               Factory s3ConfigFetcherFactory) {
        super(s3ConfigFetcherLogger, whiteListConfig.bucket(), whiteListConfig.key(), amazonS3, s3ConfigFetcherFactory,
                ITEM_COUNT);
    }

    public Map<String, Map<String, Map<String, Set<String>>>> getWhiteListItems() {
        final long now = factory.createCurrentTimeMillis();
        if (now - lastUpdateTime.get() > ONE_HOUR) {
            if (isUpdateInProgress.compareAndSet(false, true)) {
                try {
                    whiteListItems.set(readAllWhiteListItemsFromS3());
                    lastUpdateTime.set(now);
                    logger.info(SUCCESSFUL_WHITELIST_UPDATE_MSG);
                } catch (InvalidWhitelistItemInputException e) {
                    logger.error(e.getMessage(), e);
                } catch (Exception e) {
                    logger.error(ERROR_MESSAGE, e);
                } finally {
                    isUpdateInProgress.set(false);
                }
            }
        }
        return whiteListItems.get();
    }

    private Map<String, Map<String, Map<String, Set<String>>>> readAllWhiteListItemsFromS3()
            throws IOException, InvalidWhitelistItemInputException {
        try (final S3Object s3Object = amazonS3.getObject(bucket, key)) {
            final BufferedReader bufferedReader = getBufferedReader(s3Object);
            final Map<String, Map<String, Map<String, Set<String>>>> whiteListItems = new ConcurrentHashMap<>();
            SpanWhiteListItem spanWhiteListItem = (SpanWhiteListItem) readSingleWhiteListItemFromS3(bufferedReader);
            while (spanWhiteListItem != null) {
                putItemInWhiteListItems(whiteListItems, spanWhiteListItem);
                spanWhiteListItem = (SpanWhiteListItem) readSingleWhiteListItemFromS3(bufferedReader);
            }
            return whiteListItems;
        }
    }

    private void putItemInWhiteListItems(Map<String, Map<String, Map<String, Set<String>>>> whiteListItems,
                                         SpanWhiteListItem spanWhiteListItem) {
        final Map<String, Map<String, Set<String>>> finderNameMap =
                whiteListItems.computeIfAbsent(spanWhiteListItem.getFinderName(), v -> new ConcurrentHashMap<>());
        final Map<String, Set<String>> serviceNameMap =
                finderNameMap.computeIfAbsent(spanWhiteListItem.getServiceName(), v -> new ConcurrentHashMap<>());
        final Set<String> tags =
                serviceNameMap.computeIfAbsent(spanWhiteListItem.getOperationName(), v -> ConcurrentHashMap.newKeySet());
        tags.add(spanWhiteListItem.getTagName());
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

    static class SpanFactory extends S3ConfigFetcherBase.Factory<SpanWhiteListItem> {
        @Override
        SpanWhiteListItem createWhiteListItem(String... items) {
            return new SpanWhiteListItem(items[0], items[1], items[2], items[3]);
        }
    }
}
