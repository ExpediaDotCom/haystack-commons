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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcher.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcher.ERROR_MESSAGE;
import static com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcher.INVALID_DATA_MSG;
import static com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcher.SUCCESSFUL_WHITELIST_UPDATE_MSG;
import static com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcher.WHITE_LIST_ITEMS;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class S3ConfigFetcherTest {
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final String KEY = RANDOM.nextLong() + "KEY";
    private static final long ONE_HOUR = 60 * 60 * 1000L;
    private static final long MORE_THAN_ONE_HOUR = ONE_HOUR + 1 + RANDOM.nextInt(Integer.MAX_VALUE);
    private static final String FINDER_NAME = "FinderName";
    private static final String SERVICE_NAME = "ServiceName";
    private static final String OPERATION_NAME = "OperationName";
    private static final String TAG_NAME = "TagName";
    private static final String COMMENT = "Comment";
    private static final String ONE_LINE_OF_GOOD_DATA = String.format("%s;%s;%s;%s;%s",
            FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME, COMMENT);
    private static final String SECOND_LINE_OF_GOOD_DATA = String.format("%s;%s;%s;%s;%s",
            "SecondFinderName", SERVICE_NAME, OPERATION_NAME, TAG_NAME, COMMENT);
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
    private Factory mockFactory;

    @Mock
    private S3Object mockS3Object;

    @Mock
    private S3ObjectInputStream mockS3ObjectInputStream;

    @Mock
    private InputStreamReader mockInputStreamReader;

    @Mock
    private BufferedReader mockBufferedReader;

    private S3ConfigFetcher s3ConfigFetcher;
    private Factory factory;

    @Before
    public void setUp() {
        WHITE_LIST_ITEMS.set(new ConcurrentHashMap<>());
        when(mockWhiteListConfig.bucket()).thenReturn(BUCKET);
        when(mockWhiteListConfig.key()).thenReturn(KEY);
        s3ConfigFetcher = new S3ConfigFetcher(
                mockS3ConfigFetcherLogger, mockWhiteListConfig, mockAmazonS3, mockFactory);
        factory = new Factory();
    }

    @After
    public void tearDown() {
        verify(mockWhiteListConfig).bucket();
        verify(mockWhiteListConfig).key();
        verifyNoMoreInteractions(mockS3Object, mockS3ObjectInputStream, mockInputStreamReader, mockBufferedReader);
        verifyNoMoreInteractions(mockS3ConfigFetcherLogger, mockWhiteListConfig, mockAmazonS3, mockFactory);
    }

    @Test
    public void testGetWhiteListItemsOneMillisecondEarly() {
        when(mockFactory.createCurrentTimeMillis()).thenReturn(ONE_HOUR);

        final Map<String, Map<String, Map<String, Set<String>>>> whiteListItems = s3ConfigFetcher.getWhiteListItems();
        assertTrue(whiteListItems.isEmpty());

        verify(mockFactory).createCurrentTimeMillis();
    }

    @Test
    public void testGetWhiteListItemsSuccessfulFetch() throws IOException {
        whensForGetWhiteListItems();
        when(mockBufferedReader.readLine()).thenReturn(ONE_LINE_OF_GOOD_DATA).thenReturn(SECOND_LINE_OF_GOOD_DATA)
                .thenReturn(null);

        s3ConfigFetcher.getWhiteListItems();

        assertTrue(s3ConfigFetcher.isTagInWhiteList(FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME));
        assertFalse(s3ConfigFetcher.isTagInWhiteList(MISSING_FINDER_NAME, SERVICE_NAME, OPERATION_NAME, TAG_NAME));
        assertFalse(s3ConfigFetcher.isTagInWhiteList(FINDER_NAME, MISSING_SERVICE_NAME, OPERATION_NAME, TAG_NAME));
        assertFalse(s3ConfigFetcher.isTagInWhiteList(FINDER_NAME, SERVICE_NAME, "MissingOperationName", TAG_NAME));
        assertFalse(s3ConfigFetcher.isTagInWhiteList(FINDER_NAME, SERVICE_NAME, OPERATION_NAME, "MissingTagName"));
        assertEquals(MORE_THAN_ONE_HOUR, s3ConfigFetcher.lastUpdateTime.get());
        assertFalse(s3ConfigFetcher.isUpdateInProgress.get());

        verifiesForGetWhiteListItems(3, 6);
        verify(mockS3ConfigFetcherLogger).info(String.format(SUCCESSFUL_WHITELIST_UPDATE_MSG, 2));
    }

    @Test
    public void testGetWhiteListItemsUpdateInProgress() throws IOException {
        s3ConfigFetcher.isUpdateInProgress.set(true);
        whensForGetWhiteListItems();
        when(mockBufferedReader.readLine()).thenReturn(ONE_LINE_OF_GOOD_DATA).thenReturn(null);

        final Map<String, Map<String, Map<String, Set<String>>>> whiteListItems = s3ConfigFetcher.getWhiteListItems();
        assertsForEmptyWhitelist(whiteListItems, true);

        verify(mockFactory).createCurrentTimeMillis();
    }

    @Test
    public void testGetWhiteListItemsExceptionReadingFromS3() throws IOException {
        final IOException ioException = new IOException("Test");
        whensForGetWhiteListItems();
        when(mockBufferedReader.readLine()).thenThrow(ioException);

        final Map<String, Map<String, Map<String, Set<String>>>> whiteListItems = s3ConfigFetcher.getWhiteListItems();
        assertsForEmptyWhitelist(whiteListItems, false);

        verifiesForGetWhiteListItems(1, 1);
        verify(mockS3ConfigFetcherLogger).error(ERROR_MESSAGE, ioException);
    }

    @Test
    public void testGetWhiteListItemsBadData() throws IOException {
        whensForGetWhiteListItems();
        when(mockBufferedReader.readLine()).thenReturn(ONE_LINE_OF_BAD_DATA).thenReturn(null);

        final Map<String, Map<String, Map<String, Set<String>>>> whiteListItems = s3ConfigFetcher.getWhiteListItems();
        assertsForEmptyWhitelist(whiteListItems, false);

        verifiesForGetWhiteListItems(1, 1);
        verify(mockS3ConfigFetcherLogger).error(eq(String.format(INVALID_DATA_MSG, ONE_LINE_OF_BAD_DATA)),
                any(S3ConfigFetcher.InvalidWhitelistItemInputException.class));
    }

    private void assertsForEmptyWhitelist(Map<String, Map<String, Map<String, Set<String>>>> whiteListItems,
                                          boolean isUpdateInProgress) {
        assertTrue(whiteListItems.isEmpty());
        assertEquals(0L, s3ConfigFetcher.lastUpdateTime.get());
        assertEquals(isUpdateInProgress, s3ConfigFetcher.isUpdateInProgress.get());
    }

    private void whensForGetWhiteListItems() {
        when(mockFactory.createCurrentTimeMillis()).thenReturn(MORE_THAN_ONE_HOUR);
        when(mockAmazonS3.getObject(anyString(), anyString())).thenReturn(mockS3Object);
        when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
        when(mockFactory.createInputStreamReader(any())).thenReturn(mockInputStreamReader);
        when(mockFactory.createBufferedReader(any())).thenReturn(mockBufferedReader);
    }

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
}
