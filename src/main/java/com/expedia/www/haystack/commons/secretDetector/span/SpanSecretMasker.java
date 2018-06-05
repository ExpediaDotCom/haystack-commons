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
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.util.VisibleForTesting;
import io.dataapps.chlorine.finder.FinderEngine;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.ERRORS_METRIC_GROUP;

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
    private final SpanNameAndCountRecorder spanNameAndCountRecorder;

    public SpanSecretMasker(String bucket, String application) {
        //noinspection LoggerInitializedWithForeignClass
        this(new FinderEngine(),
                new Factory(),
                new SpanS3ConfigFetcher(bucket, "secret-detector/whiteListItems.txt"),
                new SpanNameAndCountRecorder(LoggerFactory.getLogger(SpanNameAndCountRecorder.class), Clock.systemUTC()),
                application);
    }

    public SpanSecretMasker(FinderEngine finderEngine,
                            SpanSecretMasker.Factory spanSecretMaskerFactory,
                            SpanS3ConfigFetcher spanS3ConfigFetcher,
                            SpanNameAndCountRecorder spanNameAndCountRecorder,
                            String application) {
        super(finderEngine, spanS3ConfigFetcher);
        this.factory = spanSecretMaskerFactory;
        this.spanNameAndCountRecorder = spanNameAndCountRecorder;
        this.application = application;
    }

    private static class BuildersForTags {
        Span.Builder spanBuilder;
        Tag.Builder tagBuilder;
    }

    private Span maskSecretsInTags(Span span) {
        final List<Tag> tags = span.getTagsList();
        BuildersForTags buildersForTags = null;
        for (int tagIndex = 0; tagIndex < tags.size(); tagIndex++) {
            final Tag tag = tags.get(tagIndex);
            if (!Strings.isNullOrEmpty(tag.getVStr())) {
                final Map<String, List<String>> mapOfTypeToKeysOfSecrets = findSecrets(tag, tag.getVStr());
                if (isNonWhitelistedSecretFound(span, mapOfTypeToKeysOfSecrets)) {
                    buildersForTags = prepareForBuild(span, tag, buildersForTags);
                    buildersForTags.tagBuilder.setVStr(MASKED_BY_HAYSTACK);
                    buildIntoTags(buildersForTags, tagIndex);
                }
            } else if (!tag.getVBytes().isEmpty()) {
                @SuppressWarnings("ObjectAllocationInLoop") final String input = new String(tag.getVBytes().toByteArray());
                final Map<String, List<String>> mapOfTypeToKeysOfSecrets = findSecrets(tag, input);
                if (isNonWhitelistedSecretFound(span, mapOfTypeToKeysOfSecrets)) {
                    buildersForTags = prepareForBuild(span, tag, buildersForTags);
                    buildersForTags.tagBuilder.setVBytes(MASKED_BY_HAYSTACK_BYTE_STRING);
                    buildIntoTags(buildersForTags, tagIndex);
                }
            }
        }
        return (buildersForTags == null) ? span : buildersForTags.spanBuilder.build();
    }

    private static void buildIntoTags(BuildersForTags buildersForTags, int tagIndex) {
        buildersForTags.spanBuilder.setTags(tagIndex, buildersForTags.tagBuilder.build());
    }

    private static BuildersForTags prepareForBuild(Span span,
                                                   Tag tag,
                                                   BuildersForTags buildersForTags) {
        final BuildersForTags buildersForTagsToUseForBuildCalls =
                (buildersForTags == null) ? new BuildersForTags() : buildersForTags;
        if (buildersForTagsToUseForBuildCalls.spanBuilder == null) {
            buildersForTagsToUseForBuildCalls.spanBuilder = Span.newBuilder();
            buildersForTagsToUseForBuildCalls.spanBuilder.mergeFrom(span);
        }
        buildersForTagsToUseForBuildCalls.tagBuilder = mergeTagIntoNewTagBuilder(tag);
        return buildersForTagsToUseForBuildCalls;
    }

    private static class BuildersForLogFields extends BuildersForTags {
        Log.Builder logBuilder;
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private Span maskSecretsInLogFields(Span span) {
        BuildersForLogFields buildersForLogFields = null;
        for (int logIndex = 0; logIndex < span.getLogsList().size(); logIndex++) {
            final Log log = span.getLogs(logIndex);
            final List<Tag> tags = log.getFieldsList();
            for (int tagIndex = 0; tagIndex < tags.size(); tagIndex++) {
                final Tag tag = tags.get(tagIndex);
                if (!Strings.isNullOrEmpty(tag.getVStr())) {
                    final Map<String, List<String>> mapOfTypeToKeysOfSecrets = findSecrets(tag, tag.getVStr());
                    if (isNonWhitelistedSecretFound(span, mapOfTypeToKeysOfSecrets)) {
                        buildersForLogFields = prepareForBuild(span, tag, log, buildersForLogFields);
                        buildersForLogFields.tagBuilder.setVStr(MASKED_BY_HAYSTACK);
                        buildIntoLogFields(buildersForLogFields, logIndex, tagIndex);
                    }
                } else if (!tag.getVBytes().isEmpty()) {
                    @SuppressWarnings("ObjectAllocationInLoop") final String input =
                            new String(tag.getVBytes().toByteArray());
                    final Map<String, List<String>> mapOfTypeToKeysOfSecrets = findSecrets(tag, input);
                    if (isNonWhitelistedSecretFound(span, mapOfTypeToKeysOfSecrets)) {
                        buildersForLogFields = prepareForBuild(span, tag, log, buildersForLogFields);
                        buildersForLogFields.tagBuilder.setVBytes(MASKED_BY_HAYSTACK_BYTE_STRING);
                        buildIntoLogFields(buildersForLogFields, logIndex, tagIndex);
                    }
                }
            }
        }
        return (buildersForLogFields == null) ? span : buildersForLogFields.spanBuilder.build();
    }

    private static void buildIntoLogFields(BuildersForLogFields buildersForLogFields, int logIndex, int tagIndex) {
        buildersForLogFields.logBuilder.setFields(tagIndex, buildersForLogFields.tagBuilder.build());
        buildersForLogFields.spanBuilder.setLogs(logIndex, buildersForLogFields.logBuilder.build());
    }

    private static BuildersForLogFields prepareForBuild(Span span,
                                                        Tag tag,
                                                        Log log,
                                                        BuildersForLogFields buildersForLogFields) {
        final BuildersForLogFields buildersForLogFieldsToUseForBuildCalls =
                (buildersForLogFields == null) ? new BuildersForLogFields() : buildersForLogFields;
        if (buildersForLogFieldsToUseForBuildCalls.spanBuilder == null) {
            buildersForLogFieldsToUseForBuildCalls.spanBuilder = Span.newBuilder();
            buildersForLogFieldsToUseForBuildCalls.spanBuilder.mergeFrom(span);
        }
        buildersForLogFieldsToUseForBuildCalls.logBuilder = mergeLogIntoNewLogBuilder(log);
        buildersForLogFieldsToUseForBuildCalls.tagBuilder = mergeTagIntoNewTagBuilder(tag);
        return buildersForLogFieldsToUseForBuildCalls;
    }

    private Map<String, List<String>> findSecrets(Tag tag, String input) {
        final Map<String, List<String>> mapOfTypeToValuesOfSecrets = finderEngine.findWithType(input);
        for (Map.Entry<String, List<String>> stringListEntry : mapOfTypeToValuesOfSecrets.entrySet()) {
            stringListEntry.getValue().replaceAll(s -> tag.getKey());
        }
        return mapOfTypeToValuesOfSecrets; // but the tag values have now been replaced by the tag key
    }

    private boolean isNonWhitelistedSecretFound(Span span, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        return !mapOfTypeToKeysOfSecrets.isEmpty() && !areAllSecretsWhitelisted(span, mapOfTypeToKeysOfSecrets);
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

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "MethodWithMultipleLoops"})
    private boolean areAllSecretsWhitelisted(Span span, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        final Iterator<Map.Entry<String, List<String>>> firstLevelIterator =
                mapOfTypeToKeysOfSecrets.entrySet().iterator();
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
                for (String tagName : finderNameToKeysOfSecrets.getValue()) {
                    spanNameAndCountRecorder.add(finderName, serviceName, operationName, tagName);
                }
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
            return metricObjects.createAndRegisterResettingCounter(ERRORS_METRIC_GROUP, application,
                    finderAndServiceName.getFinderName(), finderAndServiceName.getServiceName(), COUNTER_NAME);
        }
    }
}