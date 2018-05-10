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
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.classpath.ClasspathConfigurationSource;
import org.cfg4j.source.compose.MergeConfigurationSource;
import org.cfg4j.source.system.EnvironmentVariablesConfigurationSource;

import java.nio.file.Paths;
import java.util.Collections;

@SuppressWarnings("WeakerAccess")
public class Configuration {
    public ConfigurationProvider createMergeConfigurationProvider() {
        final MergeConfigurationSource configurationSource = new MergeConfigurationSource(
                createClasspathConfigurationSource(), createEnvironmentConfigurationSource()
        );
        final ConfigurationProviderBuilder configurationProviderBuilder = new ConfigurationProviderBuilder();
        return configurationProviderBuilder.withConfigurationSource(configurationSource).build();
    }

    private ConfigurationSource createClasspathConfigurationSource() {
        return new ClasspathConfigurationSource(() -> Collections.singletonList(Paths.get("base.yaml")));
    }

    private ConfigurationSource createEnvironmentConfigurationSource() {
        final EnvironmentVariablesConfigurationSource environmentVariablesConfigurationSource =
                new EnvironmentVariablesConfigurationSource();
        return new ChangeEnvVarsToLowerCaseConfigurationSource("HAYSTACK", environmentVariablesConfigurationSource);
    }
}
