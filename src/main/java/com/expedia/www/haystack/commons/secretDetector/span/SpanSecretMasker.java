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

import com.expedia.open.tracing.Log;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;
import com.expedia.www.haystack.commons.secretDetector.DetectorBase;
import com.expedia.www.haystack.commons.secretDetector.FinderNameAndServiceName;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.google.protobuf.ByteString;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.util.VisibleForTesting;
import io.dataapps.chlorine.finder.FinderEngine;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.maven.shared.utils.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds that tag keys and field keys in a Span that contain secrets.
 */
@SuppressWarnings("WeakerAccess")
public class SpanSecretMasker extends DetectorBase implements ValueMapper<Span, Span> {
    @VisibleForTesting
    static final Map<FinderNameAndServiceName, Counter> COUNTERS = Collections.synchronizedMap(new HashMap<>());
    @VisibleForTesting
    static final String MASKED_BY_HAYSTACK = "Confidential Data Masked By Haystack";
    @VisibleForTesting
    static final byte[] MASKED_BY_HAYSTACK_BYTES = MASKED_BY_HAYSTACK.getBytes();

    private static final String COUNTER_NAME = "SECRETS";
    private static final Map<String, Map<String, FinderNameAndServiceName>> CACHED_FINDER_NAME_AND_SECRET_NAME_OBJECTS =
            new ConcurrentHashMap<>();
    private static final ByteString MASKED_BY_HAYSTACK_BYTE_STRING = ByteString.copyFrom(MASKED_BY_HAYSTACK_BYTES);
    private final Factory factory;
    private final String application;

    public SpanSecretMasker(String bucket, String application) {
        this(new FinderEngine(),
                new Factory(),
                new SpanS3ConfigFetcher(bucket, "secret-detector/whiteListItems.txt"), application);
    }

    public SpanSecretMasker(FinderEngine finderEngine,
                            Factory detectorFactory,
                            SpanS3ConfigFetcher spanS3ConfigFetcher,
                            String application) {
        super(finderEngine, spanS3ConfigFetcher);
        this.factory = detectorFactory;
        this.application = application;
    }

    private Span maskSecretsInTags(Span span) {
        final List<Tag> tags = span.getTagsList();
        Span.Builder spanBuilder = null;
        for (int tagIndex = 0; tagIndex < tags.size(); tagIndex++) {
            final Tag tag = tags.get(tagIndex);
            if (StringUtils.isNotEmpty(tag.getVStr())) {
                final Map<String, List<String>> mapOfTypeToKeysOfSecrets = getMapOfTypeToKeysOfSecrets(tag, tag.getVStr());
                if (isNonWhitelistedSecretFound(span, mapOfTypeToKeysOfSecrets)) {
                    spanBuilder = createNewSpanBuilderIfNecessary(span, spanBuilder);
                    final Tag.Builder maskedTagBuilder = mergeTagIntoNewTagBuilder(tag);
                    maskedTagBuilder.setVStr(MASKED_BY_HAYSTACK);
                    spanBuilder.setTags(tagIndex, maskedTagBuilder.build());
                }
            } else if (!tag.getVBytes().isEmpty()) {
                @SuppressWarnings("ObjectAllocationInLoop")
                final String input = new String(tag.getVBytes().toByteArray());
                final Map<String, List<String>> mapOfTypeToKeysOfSecrets = getMapOfTypeToKeysOfSecrets(tag, input);
                if (isNonWhitelistedSecretFound(span, mapOfTypeToKeysOfSecrets)) {
                    spanBuilder = createNewSpanBuilderIfNecessary(span, spanBuilder);
                    final Tag.Builder maskedTagBuilder = mergeTagIntoNewTagBuilder(tag);
                    maskedTagBuilder.setVBytes(MASKED_BY_HAYSTACK_BYTE_STRING);
                    spanBuilder.setTags(tagIndex, maskedTagBuilder.build());
                }
            }
        }
        return (spanBuilder == null) ? span : spanBuilder.build();
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private Span maskSecretsInLogFields(Span span) {
        Span.Builder spanBuilder = null;
        for (int logIndex = 0 ; logIndex < span.getLogsList().size() ; logIndex++) {
            final Log log = span.getLogs(logIndex);
            final List<Tag> tags = log.getFieldsList();
            for (int tagIndex = 0; tagIndex < tags.size(); tagIndex++) {
                final Tag tag = tags.get(tagIndex);
                if (StringUtils.isNotEmpty(tag.getVStr())) {
                    final Map<String, List<String>> mapOfTypeToKeysOfSecrets = getMapOfTypeToKeysOfSecrets(tag, tag.getVStr());
                    if (isNonWhitelistedSecretFound(span, mapOfTypeToKeysOfSecrets)) {
                        spanBuilder = createNewSpanBuilderIfNecessary(span, spanBuilder);
                        final Log.Builder logBuilder = mergeLogIntoNewLogBuilder(log);
                        final Tag.Builder maskedTagBuilder = mergeTagIntoNewTagBuilder(tag);
                        maskedTagBuilder.setVStr(MASKED_BY_HAYSTACK);
                        logBuilder.setFields(tagIndex, maskedTagBuilder.build());
                        spanBuilder.setLogs(logIndex, logBuilder.build());
                    }
                } else if (!tag.getVBytes().isEmpty()) {
                    @SuppressWarnings("ObjectAllocationInLoop") final String input = new String(tag.getVBytes().toByteArray());
                    final Map<String, List<String>> mapOfTypeToKeysOfSecrets = getMapOfTypeToKeysOfSecrets(tag, input);
                    if (isNonWhitelistedSecretFound(span, mapOfTypeToKeysOfSecrets)) {
                        spanBuilder = createNewSpanBuilderIfNecessary(span, spanBuilder);
                        final Log.Builder logBuilder = mergeLogIntoNewLogBuilder(log);
                        final Tag.Builder maskedTagBuilder = mergeTagIntoNewTagBuilder(tag);
                        maskedTagBuilder.setVBytes(MASKED_BY_HAYSTACK_BYTE_STRING);
                        logBuilder.setFields(tagIndex, maskedTagBuilder.build());
                        spanBuilder.setLogs(logIndex, logBuilder.build());
                    }
                }
            }
        }
        return (spanBuilder == null) ? span : spanBuilder.build();
    }

    private Map<String, List<String>> getMapOfTypeToKeysOfSecrets(Tag tag, String input) {
        final Map<String, List<String>> mapOfTypeToValuesOfSecrets = finderEngine.findWithType(input);
        for (Map.Entry<String, List<String>> stringListEntry : mapOfTypeToValuesOfSecrets.entrySet()) {
            stringListEntry.getValue().replaceAll(s -> tag.getKey());
        }
        return mapOfTypeToValuesOfSecrets; // but the tag values have now been replaced by the tag key
    }

    private boolean isNonWhitelistedSecretFound(Span span, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        return !mapOfTypeToKeysOfSecrets.isEmpty() && !areAllSecretsWhitelisted(span, mapOfTypeToKeysOfSecrets);
    }

    private static Span.Builder createNewSpanBuilderIfNecessary(Span span, Span.Builder spanBuilder) {
        if(spanBuilder == null) {
            final Span.Builder newSpanBuilder = Span.newBuilder();
            newSpanBuilder.mergeFrom(span);
            return newSpanBuilder;
        }
        return spanBuilder;
    }

    private static Log.Builder mergeLogIntoNewLogBuilder(Log log) {
        final Log.Builder newLogBuilder = Log.newBuilder();
        newLogBuilder.mergeFrom(log);
        return newLogBuilder;
    }

    private static Tag.Builder mergeTagIntoNewTagBuilder(Tag tag) {
        Tag.Builder maskedTagBuilder = Tag.newBuilder();
        maskedTagBuilder.mergeFrom(tag);
        return maskedTagBuilder;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean areAllSecretsWhitelisted(Span span, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        final Iterator<Map.Entry<String, List<String>>> firstLevelIterator = mapOfTypeToKeysOfSecrets.entrySet().iterator();
        final String serviceName = span.getServiceName();
        final String operationName = span.getOperationName();
        while (firstLevelIterator.hasNext()) {
            final Map.Entry<String, List<String>> finderNameToKeysOfSecrets = firstLevelIterator.next();
            final String finderName = finderNameToKeysOfSecrets.getKey();
            finderNameToKeysOfSecrets.getValue().removeIf(
                    tagName -> s3ConfigFetcher.isInWhiteList(finderName, serviceName, operationName, tagName));
            if (finderNameToKeysOfSecrets.getValue().isEmpty()) {
                firstLevelIterator.remove();
            } else {
                incrementCounter(serviceName, finderName, application);
            }
        }
        return mapOfTypeToKeysOfSecrets.isEmpty();
    }

    @SuppressWarnings("AssignmentToMethodParameter")
    @Override
    public Span apply(@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") Span span) {
        span = maskSecretsInTags(span);
        span = maskSecretsInLogFields(span);
        return span;
    }

    private void incrementCounter(String serviceName, String finderName, String application) {
        final FinderNameAndServiceName finderNameAndServiceName = CACHED_FINDER_NAME_AND_SECRET_NAME_OBJECTS
                .computeIfAbsent(finderName, (v -> new HashMap<>()))
                .computeIfAbsent(serviceName, (v -> new FinderNameAndServiceName(finderName, serviceName)));
        COUNTERS.computeIfAbsent(
                finderNameAndServiceName, (counter -> factory.createCounter(finderNameAndServiceName, application)))
                .increment();
    }

    public static class Factory {
        private final MetricObjects metricObjects;

        public Factory() {
            this(new MetricObjects());
        }

        public Factory(MetricObjects metricObjects) {
            this.metricObjects = metricObjects;
        }

        Counter createCounter(FinderNameAndServiceName finderAndServiceName, String application) {
            return metricObjects.createAndRegisterResettingCounter(application,
                    finderAndServiceName.getFinderName(), finderAndServiceName.getServiceName(), COUNTER_NAME);
        }
    }
}