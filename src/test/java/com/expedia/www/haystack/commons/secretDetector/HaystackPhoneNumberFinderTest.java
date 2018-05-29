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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HaystackPhoneNumberFinderTest {
    private static final String[] VALID_PHONE_NUMBERS = {
            "1-800-555-1212", "1 (800) 555-1212", "800-555-1212", "(800) 555-1212", // US
            "+33.06.23.12.45.54",                                                   // France
            "(020) 1234 5678",                                                      // UK (London)
    };

    private static final String[] INVALID_US_PHONE_NUMBERS = {
            "4640-1234-5678-9120",
            "/minify/min-2487701102.js",
            "50.242.105.69",
            "8005551212",
            "18005551212",
    };

    @Mock
    private PhoneNumberUtil mockPhoneNumberUtil;

    private HaystackPhoneNumberFinder haystackPhoneNumberFinder;

    @Before
    public void setUp() {
        haystackPhoneNumberFinder = new HaystackPhoneNumberFinder();
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockPhoneNumberUtil);
    }

    @Test
    public void testGetName() {
        final String expected = HaystackPhoneNumberFinder.class.getSimpleName()
                .replace("Haystack", "")
                .replace("Finder", "");
        assertEquals(expected, haystackPhoneNumberFinder.getName());
    }

    @Test
    public void testFindStringValidNumbers() {
        for (String phoneNumber : VALID_PHONE_NUMBERS) {
            final List<String> strings = haystackPhoneNumberFinder.find(phoneNumber);
            assertEquals(phoneNumber, 1, strings.size());
        }
    }

    @Test
    public void testFindStringInvalidNumber() {
        for (String phoneNumber : INVALID_US_PHONE_NUMBERS) {
            final List<String> strings = haystackPhoneNumberFinder.find(phoneNumber);
            assertEquals(phoneNumber, 0, strings.size());
        }
    }

    @Test
    public void testFindStringsValidNumbers() {
        final List<String> phoneNumbers = Arrays.asList(VALID_PHONE_NUMBERS);
        final List<String> strings = haystackPhoneNumberFinder.find(phoneNumbers);
        assertEquals(VALID_PHONE_NUMBERS.length, strings.size());
        final Iterator<String> phoneNumbersIterator = phoneNumbers.iterator();
        final Iterator<String> stringsIterator = strings.iterator();
        while (phoneNumbersIterator.hasNext()) {
            assertEquals(phoneNumbersIterator.next(), stringsIterator.next());
        }
    }

    @Test
    public void testFindStringsInvalidNumbers() {
        final List<String> phoneNumbers = Arrays.asList(INVALID_US_PHONE_NUMBERS);
        final List<String> strings = haystackPhoneNumberFinder.find(phoneNumbers);
        assertEquals(0, strings.size());
    }

    @Test
    public void testFindNumberParseException() throws NumberParseException {
        when(mockPhoneNumberUtil.parseAndKeepRawInput(anyString(), anyString())).thenThrow(
                new NumberParseException(NumberParseException.ErrorType.TOO_LONG, "Test"));
        haystackPhoneNumberFinder = new HaystackPhoneNumberFinder(mockPhoneNumberUtil);

        final List<String> phoneNumbers = haystackPhoneNumberFinder.find(VALID_PHONE_NUMBERS[0]);

        assertTrue(phoneNumbers.isEmpty());
        verify(mockPhoneNumberUtil).parseAndKeepRawInput(VALID_PHONE_NUMBERS[0], "US");
    }
}
