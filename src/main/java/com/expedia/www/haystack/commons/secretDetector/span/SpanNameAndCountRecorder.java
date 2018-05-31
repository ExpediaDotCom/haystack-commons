package com.expedia.www.haystack.commons.secretDetector.span;

import com.expedia.open.tracing.Span;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class facilitates the logging of information about which span tags contain confidential data, and how often
 * a particular tag is seen. Typical usage:
 * <ul>
 *     <li>Call add() whenever a secret is found.</li>
 *     <li>Log the output returned by toString() at a regular interval.</li>
 *     <li>Call clear() (typically after calling toString()) to clear the counts.</li>
 * </ul>
 * The class is thread safe but spans seen between the calls (by other threads, obviously) to toString() and clear()
 * will not be tracked.
 */
public class SpanNameAndCountRecorder {
    @SuppressWarnings("FieldNotUsedInToString")
    //            FinderName, ServiceName, OperationName, TagName, Count
    private final Map<String, Map<String, Map<String, Map<String, AtomicInteger>>>> map = new ConcurrentHashMap<>();
    private final Action toStringAction = new ToStringAction();
    private final Action clearAction = new ClearAction();

    public void add(String finderName, String serviceName, String operationName, String tagName) {
        final Map<String, Map<String, Map<String, AtomicInteger>>> finderNameMap =
                map.computeIfAbsent(finderName, v -> new ConcurrentHashMap<>());
        final Map<String, Map<String, AtomicInteger>> operationNameMap =
                finderNameMap.computeIfAbsent(serviceName, v -> new ConcurrentHashMap<>());
        final Map<String, AtomicInteger> tagNameMap =
                operationNameMap.computeIfAbsent(operationName, v -> new ConcurrentHashMap<>());
        final AtomicInteger count =
                tagNameMap.computeIfAbsent(tagName, v -> new AtomicInteger(0));
        count.incrementAndGet();
    }

    @Override
    public String toString() {
        final Set<String> spanNamesWithCounts = (Set<String>) loop(new TreeSet<>(), toStringAction);
        return spanNamesWithCounts.toString();
    }

    public void clear() {
        loop(null, clearAction);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private Object loop(Object object, Action action) {
        for (Map.Entry<String, Map<String, Map<String, Map<String, AtomicInteger>>>> finderNameEntrySet : map.entrySet()) {
            final String finderName = finderNameEntrySet.getKey();
            final Map<String, Map<String, Map<String, AtomicInteger>>> serviceNameMap = finderNameEntrySet.getValue();
            for (Map.Entry<String, Map<String, Map<String, AtomicInteger>>> serviceNameEntrySet : serviceNameMap.entrySet()) {
                final String serviceName = serviceNameEntrySet.getKey();
                final Map<String, Map<String, AtomicInteger>> operationNameMap = serviceNameEntrySet.getValue();
                for (Map.Entry<String, Map<String, AtomicInteger>> operationNameEntrySet : operationNameMap.entrySet()) {
                    final String operationName = operationNameEntrySet.getKey();
                    final Map<String, AtomicInteger> tagNamesVsCounts = operationNameEntrySet.getValue();
                    for (final Map.Entry<String, AtomicInteger> tagNameAndCount : tagNamesVsCounts.entrySet()) {
                        action.execute(object, finderName, serviceName, operationName, tagNameAndCount);
                    }
                }
            }
        }
        return object;
    }

    @FunctionalInterface
    private interface Action {
        void execute(Object object,
                     String finderName,
                     String serviceName,
                     String operationName,
                     Map.Entry<String, AtomicInteger> tagNameAndCount);
    }

    private static class ToStringAction implements Action {
        public void execute(Object object,
                            String finderName,
                            String serviceName,
                            String operationName,
                            Map.Entry<String, AtomicInteger> tagNameAndCount) {
            final Set<String> spanNames = (Set<String>) object;
            final String spanName = String.format("%s;%s;%s;%s",
                    finderName, serviceName, operationName, tagNameAndCount);
            spanNames.add(spanName);
        }
    }

    private static class ClearAction implements Action {
        @Override
        public void execute(Object object,
                            String finderName,
                            String serviceName,
                            String operationName,
                            Map.Entry<String, AtomicInteger> tagNameAndCount) {
            tagNameAndCount.getValue().set(0);
        }
    }
}
