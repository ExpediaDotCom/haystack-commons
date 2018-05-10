/*
 * Copyright 2017 Expedia, Inc.
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

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("WeakerAccess")
public class ConfigurationTest {
    private final static String WHITELIST_BUCKET = "haystack.secretsnotifications.whitelist.bucket";
    private final static String WHITELIST_KEY = "haystack.secretsnotifications.whitelist.key";

    private final static Object[][] TEST_DATA = {
            {WHITELIST_BUCKET, "haystack-config"},
            {WHITELIST_KEY, "secret-detector/whiteListItems.txt"},
    };
    private static final char UNDERSCORE = '_';
    private static final char PERIOD = '.';
    private Configuration configuration;

    @Before
    public void setUp() {
        configuration = new Configuration();
    }

    @Test
    public void testCreateMergeConfigurationProvider() {
        final ConfigurationProvider configurationProvider = configuration.createMergeConfigurationProvider();

        final Properties properties = configurationProvider.allConfigurationAsProperties();
        verifyExpectedFileConfigurationsFoundThenRemove(properties);
        verifyEnvironmentVariablesAreInConfigurationPropertiesThenRemove(properties);
        verifyConfigurationsAreNowEmpty(properties);
    }

    private void verifyExpectedFileConfigurationsFoundThenRemove(Properties properties) {
        for (Object[] objects : TEST_DATA) {
            assertEquals(objects[1], properties.remove(objects[0]));
        }
    }

    private void verifyEnvironmentVariablesAreInConfigurationPropertiesThenRemove(Properties properties) {
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            final String keyWithPeriodsInsteadOfUnderscores = entry.getKey().replace(UNDERSCORE, PERIOD);
            assertEquals(entry.getValue(), properties.remove(keyWithPeriodsInsteadOfUnderscores));
        }
    }

    private void verifyConfigurationsAreNowEmpty(Properties properties) {
        assertEquals(0, properties.size());
    }
}
