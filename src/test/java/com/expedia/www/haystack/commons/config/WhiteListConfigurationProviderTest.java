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
package com.expedia.www.haystack.commons.config;

import org.cfg4j.provider.ConfigurationProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WhiteListConfigurationProviderTest {
    private WhiteListConfigurationProvider whiteListConfigurationProvider;

    @Before
    public void setUp() {
        final Configuration configuration = new Configuration();
        final ConfigurationProvider mergeConfigurationProvider = configuration.createMergeConfigurationProvider();
        whiteListConfigurationProvider = new WhiteListConfigurationProvider(mergeConfigurationProvider);
    }

    @Test
    public void testBucket() {
        assertEquals("haystack-config", whiteListConfigurationProvider.bucket());
    }

    @Test
    public void testKey() {
        assertEquals("secret-detector/whiteListItems.txt", whiteListConfigurationProvider.key());
    }
}
