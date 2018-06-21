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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("WeakerAccess")
public class S3ConfigFetcher extends S3ConfigFetcherBase {
    private static final int ITEM_COUNT = 2;

    public S3ConfigFetcher(String bucket, String key) {
        super(LoggerFactory.getLogger(S3ConfigFetcher.class), bucket, key,
                AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2).build(), new SpanFactory(), ITEM_COUNT);
    }

    public S3ConfigFetcher(Logger s3ConfigFetcherLogger,
                           WhiteListConfig whiteListConfig,
                           AmazonS3 amazonS3,
                           SpanFactory s3ConfigFetcherFactory) {
        super(s3ConfigFetcherLogger, whiteListConfig.bucket(), whiteListConfig.key(), amazonS3, s3ConfigFetcherFactory,
                ITEM_COUNT);
    }

    public boolean isInWhiteList(String... strings) {
        @SuppressWarnings("unchecked") final Map<String, Set<String>> finderNameMap =
                (Map<String, Set<String>>) getWhiteListItems();
        final Set<String> xmlSet = finderNameMap.get(strings[0]);
        if (xmlSet != null) {
            return xmlSet.contains(strings[1]);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void putItemInWhiteList(Object whiteList, WhiteListItemBase whiteListItem) {
        final WhiteListItem xmlWhiteListItem = (WhiteListItem) whiteListItem;
        final Map<String, Set<String>> whiteListCastAsMap = (Map<String, Set<String>>) whiteList;
        final Set<String> xmlPaths = whiteListCastAsMap.computeIfAbsent(
                xmlWhiteListItem.getFinderName(), v -> ConcurrentHashMap.newKeySet());
        xmlPaths.add(xmlWhiteListItem.getPath());
    }

    public static class SpanFactory extends Factory<WhiteListItem> {
        @Override
        public WhiteListItem createWhiteListItem(String... items) {
            return new WhiteListItem(items[0], items[1]);
        }

        @Override
        public Object createWhiteList() {
            return new ConcurrentHashMap<String, Set<String>>();
        }
    }
}
