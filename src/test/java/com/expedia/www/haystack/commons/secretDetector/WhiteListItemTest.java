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

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class WhiteListItemTest {
    private static final String FINDER_NAME = RANDOM.nextLong() + "FINDER_NAME";
    private static final String SERVICE_NAME = RANDOM.nextLong() + "SERVICE_NAME";
    private static final String OPERATION_NAME = RANDOM.nextLong() + "OPERATION_NAME";
    private static final String TAG_NAME = RANDOM.nextLong() + "TAG_NAME";
    private WhiteListItem whiteListItem;
    
    @Before
    public void setUp() {
        whiteListItem = new WhiteListItem(FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME);
    }

    @Test
    public void testEqualsAndHashcodeNullOther() {
        //noinspection SimplifiableJUnitAssertion,ConstantConditions,ObjectEqualsNull
        assertFalse(whiteListItem.equals(null));
    }

    @Test
    public void testEqualsSameOther() {
        assertEquals(whiteListItem, whiteListItem);
    }

    @Test
    public void testEqualsDifferentClassOther() {
        //noinspection SimplifiableJUnitAssertion,EqualsBetweenInconvertibleTypes
        assertFalse(whiteListItem.equals(FINDER_NAME));
    }

    @Test
    public void testEqualsAndHashcodeTotalMatch() {
        final WhiteListItem whiteListItem
                = new WhiteListItem(FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME);
        assertEquals(this.whiteListItem, whiteListItem);
        assertEquals(this.whiteListItem.hashCode(), whiteListItem.hashCode());
    }

    @Test
    public void testEqualsAndHashcodeFinderNameMismatch() {
        testEqualsAndHashcode(
                new WhiteListItem("1", "2", "3", "4"),
                new WhiteListItem("5", "2", "3", "4"));
    }

    @Test
    public void testEqualsAndHashcodeServiceNameMismatch() {
        testEqualsAndHashcode(
                new WhiteListItem("1", "2", "3", "4"),
                new WhiteListItem("1", "6", "3", "4"));
    }

    @Test
    public void testEqualsAndHashcodeOperationNameMismatch() {
        testEqualsAndHashcode(
                new WhiteListItem("1", "2", "3", "4"),
                new WhiteListItem("1", "2", "7", "4"));
    }

    @Test
    public void testEqualsAndHashcodeTagNameMismatch() {
        testEqualsAndHashcode(
                new WhiteListItem("1", "2", "3", "4"),
                new WhiteListItem("1", "2", "3", "8"));
    }

    private void testEqualsAndHashcode(WhiteListItem whiteListItem1, WhiteListItem whiteListItem2) {
        assertNotEquals(whiteListItem1, whiteListItem2);
        assertNotEquals(whiteListItem1.hashCode(), whiteListItem2.hashCode());
    }
}
