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
package com.expedia.www.haystack.commons.secretDetector.span;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcherBase;
import com.expedia.www.haystack.commons.secretDetector.WhiteListConfig;
import com.expedia.www.haystack.commons.secretDetector.WhiteListItemBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("WeakerAccess")
public class SpanS3ConfigFetcher extends S3ConfigFetcherBase {
    private static final int ITEM_COUNT = 4;

    public SpanS3ConfigFetcher(String bucket, String key) {
        super(LoggerFactory.getLogger(SpanS3ConfigFetcher.class), bucket, key,
                AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2).build(), new SpanFactory(), ITEM_COUNT);
    }

    public SpanS3ConfigFetcher(Logger s3ConfigFetcherLogger,
                               WhiteListConfig whiteListConfig,
                               AmazonS3 amazonS3,
                               SpanFactory s3ConfigFetcherFactory) {
        super(s3ConfigFetcherLogger, whiteListConfig.bucket(), whiteListConfig.key(), amazonS3, s3ConfigFetcherFactory,
                ITEM_COUNT);
    }

    public boolean isInWhiteList(String... strings) {
        @SuppressWarnings("unchecked")
        final Map<String, Map<String, Map<String, Set<String>>>> finderNameMap =
                (Map<String, Map<String, Map<String, Set<String>>>>) getWhiteListItems();
        final Map<String, Map<String, Set<String>>> serviceNameMap = finderNameMap.get(strings[0]);
        if (serviceNameMap != null) {
            final Map<String, Set<String>> operationNameMap = serviceNameMap.get(strings[1]);
            if (operationNameMap != null) {
                final Set<String> tagNameSet = operationNameMap.get(strings[2]);
                if (tagNameSet != null) {
                    return tagNameSet.contains(strings[3]);
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void putItemInWhiteList(Object whiteList, WhiteListItemBase whiteListItem) {
        final SpanWhiteListItem spanWhiteListItem = (SpanWhiteListItem) whiteListItem;
        final Map<String, Map<String, Map<String, Set<String>>>> whiteListCastAsMap =
                (Map<String, Map<String, Map<String, Set<String>>>>) whiteList;
        final Map<String, Map<String, Set<String>>> finderNameMap =
                whiteListCastAsMap.computeIfAbsent(spanWhiteListItem.getFinderName(), v -> new ConcurrentHashMap<>());
        final Map<String, Set<String>> serviceNameMap =
                finderNameMap.computeIfAbsent(spanWhiteListItem.getServiceName(), v -> new ConcurrentHashMap<>());
        final Set<String> tags =
                serviceNameMap.computeIfAbsent(spanWhiteListItem.getOperationName(), v -> ConcurrentHashMap.newKeySet());
        tags.add(spanWhiteListItem.getTagName());
    }

    public static class SpanFactory extends S3ConfigFetcherBase.Factory<SpanWhiteListItem> {
        @Override
        public SpanWhiteListItem createWhiteListItem(String... items) {
            return new SpanWhiteListItem(items[0], items[1], items[2], items[3]);
        }

        @Override
        public Object createWhiteList() {
            return new ConcurrentHashMap<String, Map<String, Map<String, Map<String, Set<String>>>>>();
        }
    }
}
