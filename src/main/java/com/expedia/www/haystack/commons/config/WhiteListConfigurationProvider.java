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

import com.expedia.www.haystack.commons.secretDetector.WhiteListConfig;
import org.cfg4j.provider.ConfigurationProvider;

@SuppressWarnings("WeakerAccess")
public class WhiteListConfigurationProvider implements WhiteListConfig {
    private static final String HAYSTACK_WHITELIST_CONFIG_PREFIX = "haystack.secretsnotifications.whitelist";

    private final WhiteListConfig whiteListConfig;

    public WhiteListConfigurationProvider(ConfigurationProvider configurationProvider) {
        this(configurationProvider.bind(HAYSTACK_WHITELIST_CONFIG_PREFIX, WhiteListConfig.class));
    }

    WhiteListConfigurationProvider(WhiteListConfig whiteListConfig) {
        this.whiteListConfig = whiteListConfig;
    }

    @Override
    public String bucket() {
        return whiteListConfig.bucket();
    }

    @Override
    public String key() {
        return whiteListConfig.key();
    }
}
