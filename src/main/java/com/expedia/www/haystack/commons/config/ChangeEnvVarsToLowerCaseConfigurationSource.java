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

import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.system.EnvironmentVariablesConfigurationSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * {@link ConfigurationSource} providing all environment variables under the specified {@link Environment}
 * namespaced prefix. Unfortunately cfg4j's EnvironmentVariablesConfigurationSource replaces _ with . in the environment
 * variable keys, but does not convert the upper case keys (by convention environment variable keys are in upper case
 * but this rule is not always followed) to lower case, which is necessary to permit Haystack's configuration system,
 * which uses environment variables, to work properly. Others have suggested this behavior would be "correct": see
 * https://github.com/cfg4j/cfg4j/issues/207.
 */
public class ChangeEnvVarsToLowerCaseConfigurationSource implements ConfigurationSource {

    private final String prefixOfStringsToConvertToLowerCase;
    private final EnvironmentVariablesConfigurationSource environmentVariablesConfigurationSource;

    /**
     * Constructs a new ChangeEnvVarsToLowerCaseConfigurationSource object that will convert environment variable
     * keys (not their values) to lower case if they start with the specified prefix. Since environment variables are
     * by convention upper case, the prefix will probably be upper case as well, but this is not required.
     *
     * @param prefixOfStringsToConvertToLowerCase     indicates which environment variables should be lower cased.
     * @param environmentVariablesConfigurationSource does the work (except for lower case conversion) via delegation.
     */
    ChangeEnvVarsToLowerCaseConfigurationSource(
            String prefixOfStringsToConvertToLowerCase,
            EnvironmentVariablesConfigurationSource environmentVariablesConfigurationSource) {
        this.prefixOfStringsToConvertToLowerCase = prefixOfStringsToConvertToLowerCase;
        environmentVariablesConfigurationSource.init();
        this.environmentVariablesConfigurationSource = environmentVariablesConfigurationSource;
    }

    @Override
    public Properties getConfiguration(Environment environment) {
        final Properties properties = environmentVariablesConfigurationSource.getConfiguration(environment);
        return lowerCaseKeysThatStartWithPrefix(properties, prefixOfStringsToConvertToLowerCase);
    }

    static Properties lowerCaseKeysThatStartWithPrefix(Properties properties,
                                                       String prefixOfStringsToConvertToLowerCase) {
        final Iterator<Map.Entry<Object, Object>> iterator = properties.entrySet().iterator();
        final Map<String, Object> toAdd = new HashMap<>();
        while (iterator.hasNext()) {
            final Map.Entry<Object, Object> next = iterator.next();
            final String key = (String) next.getKey();
            if (key.startsWith(prefixOfStringsToConvertToLowerCase)) {
                toAdd.put(key.toLowerCase(), next.getValue());
            }
        }
        properties.putAll(toAdd);

        return properties;
    }

    @Override
    public void init() {
        environmentVariablesConfigurationSource.init();
    }

    @Override
    public String toString() {
        return "ChangeEnvVarsToLowerCaseConfigurationSource{}";
    }

    @Override
    public void reload() {
        environmentVariablesConfigurationSource.reload();
    }

}
