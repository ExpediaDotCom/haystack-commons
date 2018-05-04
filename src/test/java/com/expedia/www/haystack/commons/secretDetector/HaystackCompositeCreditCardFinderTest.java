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

import io.dataapps.chlorine.finder.CompositeFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.expedia.www.haystack.commons.secretDetector.HaystackCreditCardFinderTest.FAKE_VISA_NUMBER_THAT_PASSES_LUHN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HaystackCompositeCreditCardFinderTest {
    private static final Random RANDOM = new Random();
    private static final String STRING = RANDOM.nextLong() + "STRING";
    private static final String OUTPUT = RANDOM.nextLong() + "OUTPUT";
    private static final List<String> OUTPUTS = Collections.singletonList(OUTPUT);

    @Mock
    private CompositeFinder mockCompositeFinder;

    private HaystackCompositeCreditCardFinder haystackCompositeCreditCardFinder;

    @Before
    public void setUp() {
        haystackCompositeCreditCardFinder = new HaystackCompositeCreditCardFinder(mockCompositeFinder);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockCompositeFinder);
    }

    @Test
    public void testFindCollectionString() {
        final List<String> samples = Collections.singletonList(STRING);
        when(mockCompositeFinder.find(anyListOf(String.class))).thenReturn(OUTPUTS);

        assertSame(OUTPUTS, haystackCompositeCreditCardFinder.find(samples));

        verify(mockCompositeFinder).find(samples);
    }

    @Test
    public void testFindString() {
        when(mockCompositeFinder.find(anyString())).thenReturn(OUTPUTS);

        assertSame(OUTPUTS, haystackCompositeCreditCardFinder.find(STRING));

        verify(mockCompositeFinder).find(STRING);
    }

    @Test
    public void testGetName() {
        when(mockCompositeFinder.getName()).thenReturn(OUTPUT);

        assertSame(OUTPUT, haystackCompositeCreditCardFinder.getName());

        verify(mockCompositeFinder).getName();
    }

    @Test
    public void testThatCreditCardValueIsEntireString() {
        haystackCompositeCreditCardFinder = new HaystackCompositeCreditCardFinder();
        final String [] creditCardNumbers = {
                FAKE_VISA_NUMBER_THAT_PASSES_LUHN,
                FAKE_VISA_NUMBER_THAT_PASSES_LUHN.replace('-', ' ')
        };
        for (final String creditCardNumber : creditCardNumbers) {
            assertFalse(haystackCompositeCreditCardFinder.find(creditCardNumber).isEmpty());
            assertTrue(haystackCompositeCreditCardFinder.find("-" + creditCardNumber).isEmpty());
            assertTrue(haystackCompositeCreditCardFinder.find(creditCardNumber + "-").isEmpty());
        }
    }
}
