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
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class XmlDetectorTest {
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final FinderEngine FINDER_ENGINE = new FinderEngine();
    private static final String XML_TEMPLATE =
            "<rootElement rootAttribute='%s'>%s<childElement childAttribute='%s'>%s</childElement></rootElement>";
    private static final String NOT_A_SECRET = "NotASecret";
    private static final String EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML = "Email";

    @Mock
    private Logger mockLogger;

    @Mock
    private Counter mockCounter;

    @Mock
    private MetricObjects mockMetricObjects;

    @Mock
    private SpanS3ConfigFetcher mockSpanS3ConfigFetcher;

    private XmlDetector xmlDetector;

    @Before
    public void setUp() {
        xmlDetector = new XmlDetector(FINDER_ENGINE, mockSpanS3ConfigFetcher);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockLogger, mockCounter, mockMetricObjects, mockSpanS3ConfigFetcher);
    }

    @Test
    public void testSmallConstructor() {
        new XmlDetector(BUCKET);
    }

    @Test
    public void testFindSecretsNoSecrets() throws ParserConfigurationException, IOException, SAXException {
        Document document = createDocument(NOT_A_SECRET, NOT_A_SECRET, NOT_A_SECRET, NOT_A_SECRET);
        final Map<String, List<String>> secrets = xmlDetector.findSecrets(document);
        assertTrue(secrets.isEmpty());
    }

    @Test
    public void testFindSecretsEmailAddressInRootElementAttribute() throws ParserConfigurationException, IOException, SAXException {
        final Document document = createDocument(EMAIL_ADDRESS, NOT_A_SECRET, NOT_A_SECRET, NOT_A_SECRET);
        testFindSecretsContainsSecret(document, "#document/rootElement/rootAttribute/#text");
    }

    @Test
    public void testFindSecretsEmailAddressInRootElementText() throws ParserConfigurationException, IOException, SAXException {
        final Document document = createDocument(NOT_A_SECRET, EMAIL_ADDRESS, NOT_A_SECRET, NOT_A_SECRET);
        testFindSecretsContainsSecret(document, "#document/rootElement/#text");
    }

    @Test
    public void testFindSecretsEmailAddressInChildElementAttribute() throws ParserConfigurationException, IOException, SAXException {
        final Document document = createDocument(NOT_A_SECRET, NOT_A_SECRET, EMAIL_ADDRESS, NOT_A_SECRET);
        testFindSecretsContainsSecret(document, "#document/rootElement/childElement/childAttribute/#text");
    }

    @Test
    public void testFindSecretsEmailAddressInChildElementText() throws ParserConfigurationException, IOException, SAXException {
        final Document document = createDocument(NOT_A_SECRET, NOT_A_SECRET, NOT_A_SECRET, EMAIL_ADDRESS);
        testFindSecretsContainsSecret(document, "#document/rootElement/childElement/#text");
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

    private Document createDocument(String rootAttributeValue,
                                    String rootElementText,
                                    String childAttributeValue,
                                    String childElementText)
            throws ParserConfigurationException, IOException, SAXException {
        final String xml = String.format(XML_TEMPLATE,
                rootAttributeValue, rootElementText, childAttributeValue, childElementText);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes());
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(byteArrayInputStream);
    }

}
