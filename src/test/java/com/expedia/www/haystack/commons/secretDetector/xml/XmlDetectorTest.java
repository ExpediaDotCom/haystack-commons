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

import com.expedia.www.haystack.metrics.MetricObjects;
import com.netflix.servo.monitor.Counter;
import io.dataapps.chlorine.finder.FinderEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static com.expedia.www.haystack.commons.secretDetector.xml.XmlDetector.TEXT_TEMPLATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("MultipleExceptionsDeclaredOnTestMethod")
@RunWith(MockitoJUnitRunner.class)
public class XmlDetectorTest {
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final FinderEngine FINDER_ENGINE = new FinderEngine();
    private static final String XML_TEMPLATE =
            "<rootElement rootAttribute='%s'>%s<childElement childAttribute='%s'>%s</childElement></rootElement>";
    private static final String NOT_A_SECRET = "NotASecret";
    private static final String EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML = "Email";
    private static final Document DOCUMENT_NO_SECRETS = createDocument(
            NOT_A_SECRET, NOT_A_SECRET, NOT_A_SECRET, NOT_A_SECRET);
    private static final Document DOCUMENT_EMAIL_IN_ROOT_ATTRIBUTE = createDocument(
            EMAIL_ADDRESS, NOT_A_SECRET, NOT_A_SECRET, NOT_A_SECRET);
    private static final Document DOCUMENT_EMAIL_IN_ROOT_TEXT = createDocument(
            NOT_A_SECRET, EMAIL_ADDRESS, NOT_A_SECRET, NOT_A_SECRET);
    private static final Document DOCUMENT_EMAIL_IN_CHILD_ATTRIBUTE = createDocument(
            NOT_A_SECRET, NOT_A_SECRET, EMAIL_ADDRESS, NOT_A_SECRET);
    private static final Document DOCUMENT_EMAIL_IN_CHILD_TEXT = createDocument(
            NOT_A_SECRET, NOT_A_SECRET, NOT_A_SECRET, EMAIL_ADDRESS);
    private static final String DOCUMENT_EMAIL_IN_ROOT_ATTRIBUTE_ID = "#document/rootElement/rootAttribute/#text";
    private static final String DOCUMENT_EMAIL_IN_ROOT_TEXT_ID = "#document/rootElement/#text";
    private static final String DOCUMENT_EMAIL_IN_CHILD_ATTRIBUTE_ID = "#document/rootElement/childElement/childAttribute/#text";
    private static final String DOCUMENT_EMAIL_IN_CHILD_TEXT_ID = "#document/rootElement/childElement/#text";

    @Mock
    private Logger mockLogger;

    @Mock
    private Counter mockCounter;

    @Mock
    private MetricObjects mockMetricObjects;

    @Mock
    private XmlS3ConfigFetcher mockXmlS3ConfigFetcher;

    private XmlDetector xmlDetector;

    @Before
    public void setUp() {
        xmlDetector = new XmlDetector(FINDER_ENGINE, mockXmlS3ConfigFetcher);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockLogger, mockCounter, mockMetricObjects, mockXmlS3ConfigFetcher);
    }

    @Test
    public void testSmallConstructor() {
        new XmlDetector(BUCKET);
    }

    @Test
    public void testFindSecretsNoSecrets() {
        final Map<String, List<String>> secrets = xmlDetector.findSecrets(DOCUMENT_NO_SECRETS);
        assertTrue(secrets.isEmpty());
    }

    @Test
    public void testFindSecretsEmailAddressInRootElementAttribute() {
        testFindSecretsContainsSecret(DOCUMENT_EMAIL_IN_ROOT_ATTRIBUTE, DOCUMENT_EMAIL_IN_ROOT_ATTRIBUTE_ID);
    }

    @Test
    public void testFindSecretsEmailAddressInRootElementText() {
        testFindSecretsContainsSecret(DOCUMENT_EMAIL_IN_ROOT_TEXT, DOCUMENT_EMAIL_IN_ROOT_TEXT_ID);
    }

    @Test
    public void testFindSecretsEmailAddressInChildElementAttribute() {
        testFindSecretsContainsSecret(DOCUMENT_EMAIL_IN_CHILD_ATTRIBUTE, DOCUMENT_EMAIL_IN_CHILD_ATTRIBUTE_ID);
    }

    @Test
    public void testFindSecretsEmailAddressInChildElementText() {
        testFindSecretsContainsSecret(DOCUMENT_EMAIL_IN_CHILD_TEXT, DOCUMENT_EMAIL_IN_CHILD_TEXT_ID);
    }

    @Test
    public void testApplyNoSecret() {
        final Iterable<String> iterable = xmlDetector.apply(DOCUMENT_NO_SECRETS);
        assertFalse(iterable.iterator().hasNext());
    }

    @Test
    public void testApplyEmailAddressInRootElementAttribute() {
        testApplyContainsSecret(DOCUMENT_EMAIL_IN_ROOT_ATTRIBUTE, DOCUMENT_EMAIL_IN_ROOT_ATTRIBUTE_ID, false);
    }

    @Test
    public void testApplySecretsEmailAddressInRootElementText() {
        testApplyContainsSecret(DOCUMENT_EMAIL_IN_ROOT_TEXT, DOCUMENT_EMAIL_IN_ROOT_TEXT_ID, false);
    }

    @Test
    public void testApplySecretsEmailAddressInChildElementAttribute() {
        testApplyContainsSecret(DOCUMENT_EMAIL_IN_CHILD_ATTRIBUTE, DOCUMENT_EMAIL_IN_CHILD_ATTRIBUTE_ID, false);
    }

    @Test
    public void testApplySecretsEmailAddressInChildElementText() {
        testApplyContainsSecret(DOCUMENT_EMAIL_IN_CHILD_TEXT, DOCUMENT_EMAIL_IN_CHILD_TEXT_ID, false);
    }

    @Test
    public void testApplyEmailAddressInRootElementAttributeButInWhiteList() {
        testApplyContainsSecret(DOCUMENT_EMAIL_IN_ROOT_ATTRIBUTE, DOCUMENT_EMAIL_IN_ROOT_ATTRIBUTE_ID, true);
    }

    @Test
    public void testApplySecretsEmailAddressInRootElementTextButInWhiteList() {
        testApplyContainsSecret(DOCUMENT_EMAIL_IN_ROOT_TEXT, DOCUMENT_EMAIL_IN_ROOT_TEXT_ID, true);
    }

    @Test
    public void testApplySecretsEmailAddressInChildElementAttributeButInWhiteList() {
        testApplyContainsSecret(DOCUMENT_EMAIL_IN_CHILD_ATTRIBUTE, DOCUMENT_EMAIL_IN_CHILD_ATTRIBUTE_ID, true);
    }

    @Test
    public void testApplySecretsEmailAddressInChildElementTextButInWhiteList() {
        testApplyContainsSecret(DOCUMENT_EMAIL_IN_CHILD_TEXT, DOCUMENT_EMAIL_IN_CHILD_TEXT_ID, true);
    }

    private void testApplyContainsSecret(Document document, String id, boolean isInWhitelist) {
        when(mockXmlS3ConfigFetcher.isInWhiteList(anyString(), anyString())).thenReturn(isInWhitelist);
        final Iterable<String> iterable = xmlDetector.apply(document);
        if(isInWhitelist) {
            assertFalse(iterable.iterator().hasNext());
        } else {
            final String next = iterable.iterator().next();
            final String finderNameAndIdWrappedInArrayAndMapSyntax =
                    '{' + EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML + "=[" + id + "]}";
            assertEquals(String.format(TEXT_TEMPLATE, finderNameAndIdWrappedInArrayAndMapSyntax), next);
        }
        verify(mockXmlS3ConfigFetcher).isInWhiteList("Email", id);
    }

    private void testFindSecretsContainsSecret(Document document, String expected) {
        final Map<String, List<String>> secrets = xmlDetector.findSecrets(document);
        assertEquals(1, secrets.size());
        for (Map.Entry<String, List<String>> finderNameToSecrets : secrets.entrySet()) {
            assertEquals(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, finderNameToSecrets.getKey());
            final List<String> value = finderNameToSecrets.getValue();
            assertEquals(1, value.size());
            assertEquals(expected, value.get(0));
        }
    }

    private static Document createDocument(String rootAttributeValue,
                                           String rootElementText,
                                           String childAttributeValue,
                                           String childElementText) {
        final String xml = String.format(XML_TEMPLATE,
                rootAttributeValue, rootElementText, childAttributeValue, childElementText);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes());
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(byteArrayInputStream);
        } catch (Exception e) {
            fail("Exception creating document for test: this should never happen. e.getMessage=" + e.getMessage());
        }
        return null;
    }
}
