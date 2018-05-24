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

import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.context.environment.ImmutableEnvironment;
import org.cfg4j.source.system.EnvironmentVariablesConfigurationSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

import static com.expedia.www.haystack.commons.config.ChangeEnvVarsToLowerCaseConfigurationSource.lowerCaseKeysThatStartWithPrefix;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ChangeEnvVarsToLowerCaseConfigurationSourceTest {
    private static final Environment ENVIRONMENT = new ImmutableEnvironment("");
    private static final String PREFIX_OF_STRINGS_TO_CONVERT_TO_LOWER_CASE = "HAYSTACK";
    private static final String CONFIGURATION_NAME_UPPER_CASE = PREFIX_OF_STRINGS_TO_CONVERT_TO_LOWER_CASE + "_TEST";
    private static final String CONFIGURATION_NAME_LOWER_CASE = CONFIGURATION_NAME_UPPER_CASE.toLowerCase(ENGLISH);
    @SuppressWarnings("CallToNumericToString")
    private static final String CONFIGURATION_VALUE = Boolean.valueOf(RANDOM.nextBoolean()).toString();

    @Mock
    private EnvironmentVariablesConfigurationSource mockEnvironmentVariablesConfigurationSource;

    private int initCallCount = 1;
    private ChangeEnvVarsToLowerCaseConfigurationSource changeEnvVarsToLowerCaseConfigurationSource;

    @Before
    public void setUp() {
        putHaystackTestIntoEnvironmentVariables();
        changeEnvVarsToLowerCaseConfigurationSource =
                new ChangeEnvVarsToLowerCaseConfigurationSource(
                        PREFIX_OF_STRINGS_TO_CONVERT_TO_LOWER_CASE, mockEnvironmentVariablesConfigurationSource);
    }

    @After
    public void tearDown() {
        removeEnvironmentVariables(CONFIGURATION_NAME_UPPER_CASE, CONFIGURATION_NAME_LOWER_CASE);
        verify(mockEnvironmentVariablesConfigurationSource, times(initCallCount)).init();

        Mockito.verifyNoMoreInteractions(mockEnvironmentVariablesConfigurationSource);
    }

    @Test
    public void testGetConfiguration() {
        final Properties copyOfCf4jProperties = new Properties();
        //noinspection UseOfPropertiesAsHashtable
        copyOfCf4jProperties.putAll(System.getenv());
        Mockito.when(mockEnvironmentVariablesConfigurationSource.getConfiguration(ENVIRONMENT))
                .thenReturn(copyOfCf4jProperties);

        final Properties configuration = changeEnvVarsToLowerCaseConfigurationSource.getConfiguration(ENVIRONMENT);

        verify(mockEnvironmentVariablesConfigurationSource).getConfiguration(ENVIRONMENT);
        verify(mockEnvironmentVariablesConfigurationSource).init();
        assertLowerCaseKeyIsPresentInDestination(configuration);
        assertUpperCaseKeyIsStillPresentInDestination(configuration);
        assertSourceAndDestinationValuesAreEqual(copyOfCf4jProperties, configuration);
    }

    private static void assertLowerCaseKeyIsPresentInDestination(Map destination) {
        final String format = "Destination should contain %s; its keys are %s";
        final String failureMessage = String.format(format, CONFIGURATION_NAME_LOWER_CASE, destination.keySet());
        assertTrue(failureMessage, destination.containsKey(CONFIGURATION_NAME_LOWER_CASE));
    }

    private static void assertUpperCaseKeyIsStillPresentInDestination(Properties destination) {
        assertNotNull(destination.getProperty(CONFIGURATION_NAME_UPPER_CASE));
    }

    private static void assertSourceAndDestinationValuesAreEqual(Properties source, Properties destination) {
        final String expected = source.getProperty(CONFIGURATION_NAME_UPPER_CASE);
        final String lowerCaseKey = CONFIGURATION_NAME_UPPER_CASE.toLowerCase(ENGLISH);
        final String actual = destination.getProperty(lowerCaseKey);
        assertEquals(expected, actual);
    }

    @Test
    public void testInit() {
        changeEnvVarsToLowerCaseConfigurationSource.init();

        initCallCount = 2;
    }

    @Test
    public void testToString() {
        final String actual = changeEnvVarsToLowerCaseConfigurationSource.toString();

        assertEquals(ChangeEnvVarsToLowerCaseConfigurationSource.class.getSimpleName() + "{}", actual);
    }

    @Test
    public void testReload() {
        changeEnvVarsToLowerCaseConfigurationSource.reload();

        verify(mockEnvironmentVariablesConfigurationSource).reload();
    }

    @Test
    public void testLowerCaseKeysThatStartWithPrefix() {
        final Properties properties = new Properties();
        final String prefix = "FOO";
        final String value1 = "1";
        final String matchingKey = prefix + value1;
        properties.setProperty(matchingKey, value1);
        final String value2 = "2";
        final String nonMatchingKey = "foo";
        properties.setProperty(nonMatchingKey, value2);

        final Properties actual = lowerCaseKeysThatStartWithPrefix(properties, prefix);

        assertEquals(3, actual.size());
        assertEquals(value1, actual.getProperty(matchingKey.toLowerCase(ENGLISH)));
        assertEquals(value2, actual.getProperty(nonMatchingKey));
    }

    private static void putHaystackTestIntoEnvironmentVariables() {
        try {
            final Map<String, String> unmodifiableEnv = System.getenv();
            final Class<?> cl = unmodifiableEnv.getClass();

            // It is not intended that environment variables be changed after the JVM starts, thus reflection
            @SuppressWarnings("JavaReflectionMemberAccess") final Field field = cl.getDeclaredField("m");
            field.setAccessible(true);

            @SuppressWarnings("unchecked") final Map<String, String> modifiableEnv =
                    (Map<String, String>) field.get(unmodifiableEnv);
            modifiableEnv.put(CONFIGURATION_NAME_UPPER_CASE, CONFIGURATION_VALUE);
            field.setAccessible(false);
        } catch (Exception e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new RuntimeException("Unable to access writable environment variable map.");
        }
    }

    private static void removeEnvironmentVariables(String... valuesToRemove) {
        try {
            final Map<String, String> unmodifiableEnv = System.getenv();
            final Class<?> cl = unmodifiableEnv.getClass();

            // It is not intended that environment variables be changed after the JVM starts, thus reflection
            @SuppressWarnings("JavaReflectionMemberAccess") final Field field = cl.getDeclaredField("m");
            field.setAccessible(true);

            @SuppressWarnings("unchecked") final Map<String, String> modifiableEnv = (
                    Map<String, String>) field.get(unmodifiableEnv);
            for (final String valueToRemove : valuesToRemove) {
                modifiableEnv.remove(valueToRemove);
            }
            field.setAccessible(false);
        } catch (Exception e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new RuntimeException("Unable to access writable environment variable map.");
        }
    }

}
