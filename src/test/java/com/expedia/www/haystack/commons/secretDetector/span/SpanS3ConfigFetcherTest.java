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
package com.expedia.www.haystack.commons.secretDetector.span;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcherBase;
import com.expedia.www.haystack.commons.secretDetector.WhiteListConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcherBase.SUCCESSFUL_WHITELIST_UPDATE_MSG;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanS3ConfigFetcher.ERROR_MESSAGE;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanS3ConfigFetcher.INVALID_DATA_MSG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpanS3ConfigFetcherTest {
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final String KEY = RANDOM.nextLong() + "KEY";
    private static final long ONE_HOUR = 60L * 60L * 1000L;
    private static final long MORE_THAN_ONE_HOUR = ONE_HOUR + 1L + RANDOM.nextInt(Integer.MAX_VALUE);
    private static final String FINDER_NAME = "FinderName";
    private static final String SERVICE_NAME = "ServiceName";
    private static final String OPERATION_NAME = "OperationName";
    private static final String TAG_NAME = "TagName";
    private static final String COMMENT = "Comment";
    private static final String ONE_LINE_OF_GOOD_DATA = String.format("%s;%s;%s;%s;%s",
            FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME, COMMENT);
    private static final SpanWhiteListItem SPAN_WHITE_LIST_ITEM =
            new SpanWhiteListItem(FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME);
    private static final String SECOND_FINDER_NAME = "SecondFinderName";
    private static final String SECOND_LINE_OF_GOOD_DATA = String.format("%s;%s;%s;%s;%s",
            SECOND_FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME, COMMENT);
    private static final SpanWhiteListItem SECOND_SPAN_WHITE_LIST_ITEM =
            new SpanWhiteListItem(SECOND_FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME);
    private static final String ONE_LINE_OF_BAD_DATA = String.format("%s;%s;%s",
            FINDER_NAME, SERVICE_NAME, OPERATION_NAME);
    private static final String MISSING_FINDER_NAME = "MissingFinderName";
    private static final String MISSING_SERVICE_NAME = "MissingServiceName";

    @Mock
    private Logger mockS3ConfigFetcherLogger;

    @Mock
    private WhiteListConfig mockWhiteListConfig;

    @Mock
    private AmazonS3 mockAmazonS3;

    @Mock
    private SpanS3ConfigFetcher.SpanFactory mockFactory;

    @Mock
    private S3Object mockS3Object;

    @Mock
    private S3ObjectInputStream mockS3ObjectInputStream;

    @Mock
    private InputStreamReader mockInputStreamReader;

    @Mock
    private BufferedReader mockBufferedReader;

    private SpanS3ConfigFetcher spanS3ConfigFetcher;
    private SpanS3ConfigFetcher.SpanFactory factory;
    private int wantedNumberOfInvocationsCreateWhiteList = 1;

    @Before
    public void setUp() {
        factory = new SpanS3ConfigFetcher.SpanFactory();
        when(mockWhiteListConfig.bucket()).thenReturn(BUCKET);
        when(mockWhiteListConfig.key()).thenReturn(KEY);
        spanS3ConfigFetcher = new SpanS3ConfigFetcher(
                mockS3ConfigFetcherLogger, mockWhiteListConfig, mockAmazonS3, mockFactory);
    }

    @SuppressWarnings("Duplicates")
    @After
    public void tearDown() {
        verify(mockFactory, times(wantedNumberOfInvocationsCreateWhiteList)).createWhiteList();
        verify(mockWhiteListConfig).bucket();
        verify(mockWhiteListConfig).key();
        verifyNoMoreInteractions(mockS3Object, mockS3ObjectInputStream, mockInputStreamReader, mockBufferedReader);
        verifyNoMoreInteractions(mockS3ConfigFetcherLogger, mockWhiteListConfig, mockAmazonS3, mockFactory);
    }

    @Test
    public void testSmallConstructor() {
        new SpanS3ConfigFetcher(BUCKET, KEY);
    }

    @Test
    public void testGetWhiteListItemsOneMillisecondEarly() {
        when(mockFactory.createCurrentTimeMillis()).thenReturn(ONE_HOUR);

        final Map<String, Map<String, Map<String, Set<String>>>> whiteList =
                (Map<String, Map<String, Map<String, Set<String>>>>) spanS3ConfigFetcher.getWhiteListItems();
        assertNull(whiteList);

        verify(mockFactory).createCurrentTimeMillis();
    }

    @Test
    public void testGetWhiteListItemsSuccessfulFetch() throws IOException {
        wantedNumberOfInvocationsCreateWhiteList = 2;
        whensForGetWhiteListItems();
        when(mockBufferedReader.readLine()).thenReturn(ONE_LINE_OF_GOOD_DATA)
                .thenReturn(SECOND_LINE_OF_GOOD_DATA, (String) null);
        when(mockFactory.createWhiteListItem(Matchers.<String>anyVararg()))
                .thenReturn(SPAN_WHITE_LIST_ITEM, SECOND_SPAN_WHITE_LIST_ITEM);

        spanS3ConfigFetcher.getWhiteListItems();

        assertTrue(spanS3ConfigFetcher.isInWhiteList(FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME));
        assertFalse(spanS3ConfigFetcher.isInWhiteList(MISSING_FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME));
        assertFalse(spanS3ConfigFetcher.isInWhiteList(FINDER_NAME, MISSING_SERVICE_NAME, OPERATION_NAME, TAG_NAME));
        assertFalse(spanS3ConfigFetcher.isInWhiteList(FINDER_NAME, SERVICE_NAME, "MissingOperationName", TAG_NAME));
        assertFalse(spanS3ConfigFetcher.isInWhiteList(FINDER_NAME, SERVICE_NAME, OPERATION_NAME, "MissingTagName"));
        assertEquals(MORE_THAN_ONE_HOUR, spanS3ConfigFetcher.getLastUpdateTimeForTest());
        assertFalse(spanS3ConfigFetcher.isUpdateInProgressForTest());

        verifiesForGetWhiteListItems(3, 6);
        verify(mockFactory).createWhiteListItem(FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME, COMMENT);
        verify(mockFactory).createWhiteListItem(SECOND_FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME, COMMENT);
        verify(mockS3ConfigFetcherLogger).info(SUCCESSFUL_WHITELIST_UPDATE_MSG);
    }

    @Test
    public void testGetWhiteListItemsUpdateInProgress() throws IOException {
        spanS3ConfigFetcher.setUpdateInProgressForTest(true);
        whensForGetWhiteListItems();
        when(mockBufferedReader.readLine()).thenReturn(ONE_LINE_OF_GOOD_DATA).thenReturn(null);

        final Map<String, Map<String, Map<String, Set<String>>>> whiteList =
                (Map<String, Map<String, Map<String, Set<String>>>>) spanS3ConfigFetcher.getWhiteListItems();
        assertsForEmptyWhiteList(whiteList, true, 0L);

        verify(mockFactory).createCurrentTimeMillis();
    }

    @Test
    public void testGetWhiteListItemsExceptionReadingFromS3() throws IOException {
        wantedNumberOfInvocationsCreateWhiteList = 2;
        final IOException ioException = new IOException("Test");
        whensForGetWhiteListItems();
        when(mockBufferedReader.readLine()).thenThrow(ioException);

        final Map<String, Map<String, Map<String, Set<String>>>> whiteList =
                (Map<String, Map<String, Map<String, Set<String>>>>) spanS3ConfigFetcher.getWhiteListItems();
        assertsForEmptyWhiteList(whiteList, false, MORE_THAN_ONE_HOUR);

        verifiesForGetWhiteListItems(1, 1);
        verify(mockS3ConfigFetcherLogger).error(ERROR_MESSAGE, ioException);
    }

    @Test
    public void testGetWhiteListItemsBadData() throws IOException {
        wantedNumberOfInvocationsCreateWhiteList = 2;
        whensForGetWhiteListItems();
        when(mockBufferedReader.readLine()).thenReturn(ONE_LINE_OF_BAD_DATA, (String) null);

        final Map<String, Map<String, Map<String, Set<String>>>> whiteList =
                (Map<String, Map<String, Map<String, Set<String>>>>) spanS3ConfigFetcher.getWhiteListItems();
        assertsForEmptyWhiteList(whiteList, false, MORE_THAN_ONE_HOUR);

        verifiesForGetWhiteListItems(1, 1);
        verify(mockS3ConfigFetcherLogger).error(eq(String.format(INVALID_DATA_MSG, ONE_LINE_OF_BAD_DATA, 3)),
                any(S3ConfigFetcherBase.InvalidWhitelistItemInputException.class));
    }

    private void assertsForEmptyWhiteList(Map<String, Map<String, Map<String, Set<String>>>> whiteList,
                                          boolean isUpdateInProgress,
                                          long lastUpdateTime) {
        assertNull(whiteList);
        assertEquals(lastUpdateTime, spanS3ConfigFetcher.getLastUpdateTimeForTest());
        assertEquals(isUpdateInProgress, spanS3ConfigFetcher.isUpdateInProgressForTest());
    }

    private void whensForGetWhiteListItems() {
        when(mockFactory.createCurrentTimeMillis()).thenReturn(MORE_THAN_ONE_HOUR);
        when(mockAmazonS3.getObject(anyString(), anyString())).thenReturn(mockS3Object);
        when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
        when(mockFactory.createInputStreamReader(any())).thenReturn(mockInputStreamReader);
        when(mockFactory.createBufferedReader(any())).thenReturn(mockBufferedReader);
        when(mockFactory.createWhiteList())
                .thenReturn(new ConcurrentHashMap<String, Map<String, Map<String, Set<String>>>>());
    }

    @SuppressWarnings({"resource", "Duplicates"})
    private void verifiesForGetWhiteListItems(int wantedNumberOfInvocationsReadLine,
                                              int wantedNumberOfInvocationsCreateCurrentTimeMillis) throws IOException {
        verify(mockFactory, times(wantedNumberOfInvocationsCreateCurrentTimeMillis)).createCurrentTimeMillis();
        verify(mockAmazonS3).getObject(BUCKET, KEY);
        verify(mockS3Object).getObjectContent();
        verify(mockS3Object).close();
        verify(mockFactory).createInputStreamReader(mockS3ObjectInputStream);
        verify(mockFactory).createBufferedReader(mockInputStreamReader);
        verify(mockBufferedReader, times(wantedNumberOfInvocationsReadLine)).readLine();
    }

    @Test
    public void testFactoryCreateCurrentTimeMillis() {
        final long currentTimeMillis = System.currentTimeMillis();

        assertTrue(factory.createCurrentTimeMillis() >= currentTimeMillis);
    }

    @Test
    public void testFactoryCreateInputStreamReader() {
        assertNotNull(factory.createInputStreamReader(mockS3ObjectInputStream));
    }

    @Test
    public void testFactoryCreateBufferedReader() {
        assertNotNull(factory.createBufferedReader(mockInputStreamReader));
    }

    @Test
    public void testFactoryCreateWhiteListItem() {
        final SpanWhiteListItem whiteListItem = factory.createWhiteListItem(
                FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME);
        assertEquals(FINDER_NAME, whiteListItem.getFinderName());
        assertEquals(SERVICE_NAME, whiteListItem.getServiceName());
        assertEquals(OPERATION_NAME, whiteListItem.getOperationName());
        assertEquals(TAG_NAME, whiteListItem.getTagName());
    }

    @Test
    public void testGetWhiteList() {
        final Map<String, Map<String, Map<String, Map<String, Set<String>>>>> whiteList =
                (Map<String, Map<String, Map<String, Map<String, Set<String>>>>>) factory.createWhiteList();
        assertNotNull(whiteList);
    }
}
