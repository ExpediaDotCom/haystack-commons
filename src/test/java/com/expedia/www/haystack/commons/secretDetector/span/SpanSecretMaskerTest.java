package com.expedia.www.haystack.commons.secretDetector.span;

import com.expedia.open.tracing.Log;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;
import com.expedia.www.haystack.commons.secretDetector.FinderNameAndServiceName;
import com.expedia.www.haystack.commons.secretDetector.span.SpanSecretMasker.Factory;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.netflix.servo.monitor.Counter;
import io.dataapps.chlorine.finder.FinderEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.BYTES_FIELD_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.BYTES_TAG_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESSES_AND_IP_ADDRESS_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_IN_TAG_BYTES_AND_LOG_BYTES_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_LOG_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.FULLY_POPULATED_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.OPERATION_NAME;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.SERVICE_NAME;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.STRING_FIELD_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.STRING_TAG_KEY;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.COUNTER_NAME;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.ERRORS_METRIC_GROUP;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanSecretMasker.MASKED_BY_HAYSTACK;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanSecretMasker.MASKED_BY_HAYSTACK_BYTES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"CallToSuspiciousStringMethod", "ConstantConditions"})
@RunWith(MockitoJUnitRunner.class)
public class SpanSecretMaskerTest {
    private static final String APPLICATION = RANDOM.nextLong() + "APPLICATION";
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final FinderEngine FINDER_ENGINE = new FinderEngine();
    private static final String EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML = "Email";
    private static final FinderNameAndServiceName EMAIL_FINDER_NAME_AND_SERVICE_NAME =
            new FinderNameAndServiceName(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME);
    private static final String IP_FINDER_NAME = "NonLocalIpV4Address";
    private static final FinderNameAndServiceName IPV4_FINDER_NAME_AND_SERVICE_NAME =
            new FinderNameAndServiceName(IP_FINDER_NAME, SERVICE_NAME);

    @Mock
    private Factory mockFactory;

    @Mock
    private SpanS3ConfigFetcher mockSpanS3ConfigFetcher;

    @Mock
    private Counter mockCounter;

    @Mock
    private MetricObjects mockMetricObjects;

    private SpanSecretMasker spanSecretMasker;
    private Factory factory;

    @Before
    public void setUp() {
        spanSecretMasker = new SpanSecretMasker(FINDER_ENGINE, mockFactory, mockSpanS3ConfigFetcher, APPLICATION);
        factory = new Factory(mockMetricObjects);
    }

    @After
    public void tearDown() {
        SpanSecretMasker.COUNTERS.clear();
        verifyNoMoreInteractions(mockFactory, mockSpanS3ConfigFetcher, mockCounter, mockMetricObjects);
    }

    @Test
    public void testSmallConstructor() {
        new SpanSecretMasker(BUCKET, APPLICATION);
    }

    @Test
    public void testApplyNoSecret() {
        final Span span = spanSecretMasker.apply(FULLY_POPULATED_SPAN);

        assertEquals(FULLY_POPULATED_SPAN, span);
    }

    @Test
    public void testFindSecretsHaystackEmailAddressInTagString() {
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESS_SPAN);

        assertNotEquals(EMAIL_ADDRESS_SPAN, span);
        assertEquals(MASKED_BY_HAYSTACK, findTag(span, STRING_TAG_KEY).getVStr());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, STRING_TAG_KEY);
        verify(mockFactory).createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockCounter).increment();
    }

    @Test
    public void testFindSecretsHaystackEmailAddressInTagStringTagWhitelisted() {
        when(mockSpanS3ConfigFetcher.isInWhiteList(Matchers.<String>anyVararg())).thenReturn(true);
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESS_SPAN);

        assertEquals(EMAIL_ADDRESS_SPAN, span);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, STRING_TAG_KEY);
    }

    @Test
    public void testFindSecretsHaystackEmailAddressInTagStringAndTagBytesAndIpAddressInTagBytes() {
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESSES_AND_IP_ADDRESS_SPAN);

        assertNotEquals(EMAIL_ADDRESSES_AND_IP_ADDRESS_SPAN, span);
        assertEquals(MASKED_BY_HAYSTACK, findTag(span, STRING_TAG_KEY).getVStr());
        assertArrayEquals(MASKED_BY_HAYSTACK_BYTES, findTag(span, BYTES_TAG_KEY).getVBytes().toByteArray());
        assertArrayEquals(MASKED_BY_HAYSTACK_BYTES, findLogFieldTag(span, BYTES_FIELD_KEY).getVBytes().toByteArray());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_TAG_KEY);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_FIELD_KEY);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                IP_FINDER_NAME, SERVICE_NAME, OPERATION_NAME, STRING_TAG_KEY);
        verify(mockFactory).createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockFactory).createCounter(IPV4_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockCounter, times(3)).increment();
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void testFindSecretsHaystackEmailAddressInTagBytesAndLogBytes() {
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESS_IN_TAG_BYTES_AND_LOG_BYTES_SPAN);

        assertNotEquals(EMAIL_ADDRESS_IN_TAG_BYTES_AND_LOG_BYTES_SPAN, span);
        assertArrayEquals(MASKED_BY_HAYSTACK_BYTES, findTag(span, BYTES_TAG_KEY).getVBytes().toByteArray());
        assertArrayEquals(MASKED_BY_HAYSTACK_BYTES, findLogFieldTag(span, BYTES_FIELD_KEY).getVBytes().toByteArray());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_TAG_KEY);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_FIELD_KEY);
        verify(mockFactory).createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockCounter, times(2)).increment();
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void testFindSecretsHaystackEmailAddressInLogString() {
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESS_LOG_SPAN);

        assertNotEquals(EMAIL_ADDRESS_LOG_SPAN, span);
        assertEquals(MASKED_BY_HAYSTACK, findLogFieldTag(span, STRING_FIELD_KEY).getVStr());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, STRING_FIELD_KEY);
        verify(mockFactory).createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockCounter).increment();
    }

    private static Tag findTag(Span span, String key) {
        final List<Tag> tags = span.getTagsList();
        for (Tag tag : tags) {
            if (tag.getKey().equals(key)) {
                return tag;
            }
        }
        return null;
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private static Tag findLogFieldTag(Span span, String key) {
        final List<Log> logs = span.getLogsList();
        for (Log log : logs) {
            for (Tag tag : log.getFieldsList()) {
                if (tag.getKey().equals(key)) {
                    return tag;
                }
            }
        }
        return null;
    }

    @Test
    public void testFactoryCreateCounter() {
        when(mockMetricObjects.createAndRegisterResettingCounter(
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockCounter);

        final Counter counter = factory.createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);

        assertSame(counter, mockCounter);
        verify(mockMetricObjects).createAndRegisterResettingCounter(ERRORS_METRIC_GROUP,
                APPLICATION, EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, COUNTER_NAME);
    }
}
