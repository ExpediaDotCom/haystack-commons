package com.expedia.www.haystack.commons.secretDetector.span;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpanNameAndCountRecorderTest {
    // All of the *_NAMES arrays are purposely out of sorted order to verify that the SpanNameAndCountRecorder.toString() sorts
    private static final String F1 = "FinderName1";
    private static final String F2 = "FinderName2";
    private static final String[] FINDER_NAMES = {F2, F1};
    private static final String S1 = "ServiceName1";
    private static final String S2 = "ServiceName2";
    private static final String[] SERVICE_NAMES = {S2, S1};
    private static final String O1 = "OperationName1";
    private static final String O2 = "OperationName2";
    private static final String[] OPERATION_NAMES = {O2, O1};
    private static final String T1 = "TagName1";
    private static final String T2 = "TagName2";
    private static final String[] TAG_NAMES = {T2, T1};
    private static final String S = "%s;%s;%s;%s=%d";
    private static final String S16 = "[%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s]";
    private static final String FORMAT = String.format(S16, S, S, S, S, S, S, S, S, S, S, S, S, S, S, S, S);

    private SpanNameAndCountRecorder spanNameAndCountRecorder;

    @SuppressWarnings("MethodWithMultipleLoops")
    @Before
    public void setUp() {
        spanNameAndCountRecorder = new SpanNameAndCountRecorder();
        int count = 0;
        for (String finderName : FINDER_NAMES) {
            for (String serviceName : SERVICE_NAMES) {
                for (String operationName : OPERATION_NAMES) {
                    for (String tagKey : TAG_NAMES) {
                        count++;
                        for (int i = 0; i < count; i++) {
                            spanNameAndCountRecorder.add(finderName, serviceName, operationName, tagKey);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testToString() {
        final String expected = String.format(FORMAT,
                F1, S1, O1, T1, 16,
                F1, S1, O1, T2, 15,
                F1, S1, O2, T1, 14,
                F1, S1, O2, T2, 13,
                F1, S2, O1, T1, 12,
                F1, S2, O1, T2, 11,
                F1, S2, O2, T1, 10,
                F1, S2, O2, T2, 9,
                F2, S1, O1, T1, 8,
                F2, S1, O1, T2, 7,
                F2, S1, O2, T1, 6,
                F2, S1, O2, T2, 5,
                F2, S2, O1, T1, 4,
                F2, S2, O1, T2, 3,
                F2, S2, O2, T1, 2,
                F2, S2, O2, T2, 1);
        assertEquals(expected, spanNameAndCountRecorder.toString());
    }

    @Test
    public void testClear() {
        spanNameAndCountRecorder.clear();
        final String expected = String.format(FORMAT,
                F1, S1, O1, T1, 0,
                F1, S1, O1, T2, 0,
                F1, S1, O2, T1, 0,
                F1, S1, O2, T2, 0,
                F1, S2, O1, T1, 0,
                F1, S2, O1, T2, 0,
                F1, S2, O2, T1, 0,
                F1, S2, O2, T2, 0,
                F2, S1, O1, T1, 0,
                F2, S1, O1, T2, 0,
                F2, S1, O2, T1, 0,
                F2, S1, O2, T2, 0,
                F2, S2, O1, T1, 0,
                F2, S2, O1, T2, 0,
                F2, S2, O2, T1, 0,
                F2, S2, O2, T2, 0);
        assertEquals(expected, spanNameAndCountRecorder.toString());
    }
}
