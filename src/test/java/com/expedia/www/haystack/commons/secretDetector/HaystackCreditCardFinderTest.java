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

import org.junit.Before;
import org.junit.Test;

import static com.expedia.www.haystack.commons.secretDetector.HaystackCompositeCreditCardFinder.AMEX_PATTERN;
import static com.expedia.www.haystack.commons.secretDetector.HaystackCompositeCreditCardFinder.DINERS_CLUB_1_PATTERN;
import static com.expedia.www.haystack.commons.secretDetector.HaystackCompositeCreditCardFinder.DINERS_CLUB_2_PATTERN;
import static com.expedia.www.haystack.commons.secretDetector.HaystackCompositeCreditCardFinder.DISCOVER_PATTERN;
import static com.expedia.www.haystack.commons.secretDetector.HaystackCompositeCreditCardFinder.JCB_1_PATTERN;
import static com.expedia.www.haystack.commons.secretDetector.HaystackCompositeCreditCardFinder.JCB_2_PATTERN;
import static com.expedia.www.haystack.commons.secretDetector.HaystackCompositeCreditCardFinder.MASTERCARD_PATTERN;
import static com.expedia.www.haystack.commons.secretDetector.HaystackCompositeCreditCardFinder.VISA_PATTERN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HaystackCreditCardFinderTest {
    static final String FAKE_VISA_NUMBER_THAT_PASSES_LUHN = "4640-1234-5678-9120";
    private static final String NAME = RANDOM.nextLong() + "NAME";
    private static final String FAKE_VISA_NUMBER_THAT_DOES_NOT_PASS_LUHN = "4640-1234-5678-9123";
    private static final String FAKE_MC_NUMBER_THAT_PASSES_LUHN = "5500-0000 00000004";
    private static final String FAKE_AMEX_NUMBER_THAT_PASSES_LUHN = "3400-000000 00009";
    private static final String FAKE_DINERS_CLUB_1_NUMBER_THAT_PASSES_LUHN = "3000 000000-0004";
    private static final String FAKE_DINERS_CLUB_2_NUMBER_THAT_PASSES_LUHN = "3600 000000-0008";
    private static final String FAKE_DISCOVER_NUMBER_THAT_PASSES_LUHN = "60110000-0000 0004";
    private static final String FAKE_JCB_1_NUMBER_THAT_PASSES_LUHN = "3088 0000 0000 0009";
    private static final String FAKE_JCB_2_NUMBER_THAT_PASSES_LUHN = "180000000000002";

    private HaystackCreditCardFinder haystackCreditCardFinder;

    @Before
    public void setUp() {
        haystackCreditCardFinder = new HaystackCreditCardFinder(NAME, VISA_PATTERN);
    }

    @Test
    public void testFindUsesLuhnCheck() {
        assertTrue(haystackCreditCardFinder.find(FAKE_VISA_NUMBER_THAT_DOES_NOT_PASS_LUHN).isEmpty());
        assertFalse(haystackCreditCardFinder.find(FAKE_VISA_NUMBER_THAT_PASSES_LUHN).isEmpty());
    }

    @Test
    public void testMasterCard() {
        haystackCreditCardFinder = new HaystackCreditCardFinder(NAME, MASTERCARD_PATTERN);
        assertFalse(haystackCreditCardFinder.find(FAKE_MC_NUMBER_THAT_PASSES_LUHN).isEmpty());
    }

    @Test
    public void testAmericanExpress() {
        haystackCreditCardFinder = new HaystackCreditCardFinder(NAME, AMEX_PATTERN);
        assertFalse(haystackCreditCardFinder.find(FAKE_AMEX_NUMBER_THAT_PASSES_LUHN).isEmpty());
    }

    @Test
    public void testDinersClub1() {
        haystackCreditCardFinder = new HaystackCreditCardFinder(NAME, DINERS_CLUB_1_PATTERN);
        assertFalse(haystackCreditCardFinder.find(FAKE_DINERS_CLUB_1_NUMBER_THAT_PASSES_LUHN).isEmpty());
    }

    @Test
    public void testDinersClub2() {
        haystackCreditCardFinder = new HaystackCreditCardFinder(NAME, DINERS_CLUB_2_PATTERN);
        assertFalse(haystackCreditCardFinder.find(FAKE_DINERS_CLUB_2_NUMBER_THAT_PASSES_LUHN).isEmpty());
    }

    @Test
    public void testDiscover() {
        haystackCreditCardFinder = new HaystackCreditCardFinder(NAME, DISCOVER_PATTERN);
        assertFalse(haystackCreditCardFinder.find(FAKE_DISCOVER_NUMBER_THAT_PASSES_LUHN).isEmpty());
    }

    @Test
    public void testJcb1() {
        haystackCreditCardFinder = new HaystackCreditCardFinder(NAME, JCB_1_PATTERN);
        assertFalse(haystackCreditCardFinder.find(FAKE_JCB_1_NUMBER_THAT_PASSES_LUHN).isEmpty());
    }

    @Test
    public void testJcb2() {
        haystackCreditCardFinder = new HaystackCreditCardFinder(NAME, JCB_2_PATTERN);
        assertFalse(haystackCreditCardFinder.find(FAKE_JCB_2_NUMBER_THAT_PASSES_LUHN).isEmpty());
    }
}
