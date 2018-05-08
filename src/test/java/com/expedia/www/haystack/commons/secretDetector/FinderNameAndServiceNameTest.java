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

public class FinderNameAndServiceNameTest {
    private static final String FINDER_NAME = RANDOM.nextLong() + "FINDER_NAME";
    private static final String SERVICE_NAME = RANDOM.nextLong() + "SERVICE_NAME";

    private FinderNameAndServiceName finderNameAndServiceName;

    @Before
    public void setUp() {
        finderNameAndServiceName = new FinderNameAndServiceName(FINDER_NAME, SERVICE_NAME);
    }

    @Test
    public void testGetFinderName() {
        assertEquals(FINDER_NAME, finderNameAndServiceName.getFinderName());
    }

    @Test
    public void testGetServiceName() {
        assertEquals(SERVICE_NAME, finderNameAndServiceName.getServiceName());
    }

    @Test
    public void testEqualsNullOther() {
        //noinspection SimplifiableJUnitAssertion,ConstantConditions,ObjectEqualsNull
        assertFalse(finderNameAndServiceName.equals(null));
    }

    @Test
    public void testEqualsSameOther() {
        assertEquals(finderNameAndServiceName, finderNameAndServiceName);
    }

    @Test
    public void testEqualsDifferentClassOther() {
        //noinspection SimplifiableJUnitAssertion,EqualsBetweenInconvertibleTypes
        assertFalse(finderNameAndServiceName.equals(FINDER_NAME));
    }

    @Test
    public void testEqualsTotalMatch() {
        final FinderNameAndServiceName finderNameAndServiceName
                = new FinderNameAndServiceName(FINDER_NAME, SERVICE_NAME);
        assertEquals(this.finderNameAndServiceName, finderNameAndServiceName);
    }

    @Test
    public void testEqualsFinderNameMismatch() {
        final FinderNameAndServiceName finderNameAndServiceName13
                = new FinderNameAndServiceName("1", "3");
        final FinderNameAndServiceName finderNameAndServiceName23
                = new FinderNameAndServiceName("2", "3");
        assertNotEquals(finderNameAndServiceName13, finderNameAndServiceName23);
    }

    @Test
    public void testEqualsServiceNameMismatch() {
        final FinderNameAndServiceName finderNameAndServiceName12
                = new FinderNameAndServiceName("1", "2");
        final FinderNameAndServiceName finderNameAndServiceName13
                = new FinderNameAndServiceName("1", "3");
        assertNotEquals(finderNameAndServiceName12, finderNameAndServiceName13);
    }

    @Test
    public void testHashCodeFinderNameMismatch() {
        final FinderNameAndServiceName finderNameAndServiceName13
                = new FinderNameAndServiceName("1", "3");
        final FinderNameAndServiceName finderNameAndServiceName23
                = new FinderNameAndServiceName("2", "3");
        assertNotEquals(finderNameAndServiceName13.hashCode(), finderNameAndServiceName23.hashCode());
    }

    @Test
    public void testHashCodeServiceNameMismatch() {
        final FinderNameAndServiceName finderNameAndServiceName12
                = new FinderNameAndServiceName("1", "2");
        final FinderNameAndServiceName finderNameAndServiceName13
                = new FinderNameAndServiceName("1", "3");
        assertNotEquals(finderNameAndServiceName12.hashCode(), finderNameAndServiceName13.hashCode());
    }

}
