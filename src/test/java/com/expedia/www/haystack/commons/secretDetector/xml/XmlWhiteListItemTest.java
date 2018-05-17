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
package com.expedia.www.haystack.commons.secretDetector.xml;

import org.junit.Before;
import org.junit.Test;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class XmlWhiteListItemTest {
    private static final String FINDER_NAME = RANDOM.nextLong() + "FINDER_NAME";
    private static final String SERVICE_NAME = RANDOM.nextLong() + "SERVICE_NAME";
    private XmlWhiteListItem xmlWhiteListItem;

    @Before
    public void setUp() {
        xmlWhiteListItem = new XmlWhiteListItem(FINDER_NAME, SERVICE_NAME);
    }

    @Test
    public void testEqualsAndHashcodeNullOther() {
        //noinspection SimplifiableJUnitAssertion,SimplifiableJUnitAssertion,ConstantConditions,ObjectEqualsNull
        assertFalse(xmlWhiteListItem.equals(null));
    }

    @Test
    public void testEqualsSameOther() {
        assertEquals(xmlWhiteListItem, xmlWhiteListItem);
    }

    @Test
    public void testEqualsDifferentClassOther() {
        //noinspection SimplifiableJUnitAssertion,EqualsBetweenInconvertibleTypes
        assertFalse(xmlWhiteListItem.equals(FINDER_NAME));
    }

    @Test
    public void testEqualsAndHashcodeTotalMatch() {
        final XmlWhiteListItem other = new XmlWhiteListItem(FINDER_NAME, SERVICE_NAME);
        assertEquals(this.xmlWhiteListItem, other);
        assertEquals(this.xmlWhiteListItem.hashCode(), other.hashCode());
    }

    @Test
    public void testEqualsAndHashcodeFinderNameMismatch() {
        testEqualsAndHashcodeMismatch(
                new XmlWhiteListItem("1", "2"),
                new XmlWhiteListItem("3", "2"));
    }

    @Test
    public void testEqualsAndHashcodeXmlPathMismatch() {
        testEqualsAndHashcodeMismatch(
                new XmlWhiteListItem("1", "2"),
                new XmlWhiteListItem("1", "4"));
    }

    private static void testEqualsAndHashcodeMismatch(XmlWhiteListItem xmlWhiteListItem1,
                                                      XmlWhiteListItem xmlWhiteListItem2) {
        assertNotEquals(xmlWhiteListItem1, xmlWhiteListItem2);
        assertNotEquals(xmlWhiteListItem1.hashCode(), xmlWhiteListItem2.hashCode());
    }
}
