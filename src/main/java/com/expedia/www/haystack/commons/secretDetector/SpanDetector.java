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

import com.expedia.open.tracing.Log;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.util.VisibleForTesting;
import io.dataapps.chlorine.finder.FinderEngine;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.maven.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds that tag keys and field keys in a Span that contain secrets.
 */
@SuppressWarnings("WeakerAccess")
public class SpanDetector extends DetectorBase implements ValueMapper<Span, Iterable<String>> {
    private static final String TEXT_TEMPLATE =
            "Confidential data has been found in a span: service [%s] operation [%s] span [%s] trace [%s] tag(s) [%s]";
    @VisibleForTesting
    static final Set<String> FINDERS_TO_LOG = Collections.singleton("Credit_Card");
    @VisibleForTesting
    static final String ERRORS_METRIC_GROUP = "errors";
    @VisibleForTesting
    static final Map<FinderNameAndServiceName, Counter> COUNTERS = Collections.synchronizedMap(new HashMap<>());
    @VisibleForTesting
    static final String COUNTER_NAME = "SECRETS";
    private static final Map<String, Map<String, FinderNameAndServiceName>> CACHED_FINDER_NAME_AND_SECRET_NAME_OBJECTS =
            new ConcurrentHashMap<>();
    private final Logger logger;
    private final Factory factory;
    private final String application;

    public SpanDetector(String bucket, String application) {
        this(LoggerFactory.getLogger(SpanDetector.class),
                new FinderEngine(),
                new Factory(),
                new SpanS3ConfigFetcher(bucket, "secret-detector/whiteList.txt"), application);
    }

    public SpanDetector(Logger detectorLogger,
                        FinderEngine finderEngine,
                        Factory detectorFactory,
                        SpanS3ConfigFetcher spanS3ConfigFetcher,
                        String application) {
        super(finderEngine, spanS3ConfigFetcher);
        this.logger = detectorLogger;
        this.factory = detectorFactory;
        this.application = application;
    }

    public Map<String, List<String>> findSecrets(Span span) {
        final Map<String, List<String>> mapOfTypeToKeysOfSecrets = new HashMap<>();
        findSecretsInTags(mapOfTypeToKeysOfSecrets, span);
        findSecretsInLogFields(mapOfTypeToKeysOfSecrets, span);
        return mapOfTypeToKeysOfSecrets;
    }

    private void findSecretsInTags(Map<String, List<String>> mapOfTypeToKeysOfSecrets, Span span) {
        findSecrets(mapOfTypeToKeysOfSecrets, span.getTagsList());
    }

    private void findSecretsInLogFields(Map<String, List<String>> mapOfTypeToKeysOfSecrets, Span span) {
        for (final Log log : span.getLogsList()) {
            findSecrets(mapOfTypeToKeysOfSecrets, log.getFieldsList());
        }
    }

    private void findSecrets(Map<String, List<String>> mapOfTypeToKeysOfSecrets, List<Tag> tags) {
        for (final Tag tag : tags) {
            if (StringUtils.isNotEmpty(tag.getVStr())) {
                final String input = tag.getVStr();
                putKeysOfSecretsIntoMap(mapOfTypeToKeysOfSecrets, tag.getKey(), finderEngine.findWithType(input));
            } else if (!tag.getVBytes().isEmpty()) {
                @SuppressWarnings("ObjectAllocationInLoop") final String input = new String(tag.getVBytes().toByteArray());
                putKeysOfSecretsIntoMap(mapOfTypeToKeysOfSecrets, tag.getKey(), finderEngine.findWithType(input));
            }
        }
    }

    @Override
    public Iterable<String> apply(@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") Span span) {
        final Map<String, List<String>> mapOfTypeToKeysOfSecrets = findSecrets(span);
        final String serviceName = span.getServiceName();
        final String operationName = span.getOperationName();
        if (mapOfTypeToKeysOfSecrets.isEmpty()) {
            return Collections.emptyList();
        }
        final String emailText = getEmailText(span, mapOfTypeToKeysOfSecrets);
        for (Map.Entry<String, List<String>> finderNameToKeysOfSecrets : mapOfTypeToKeysOfSecrets.entrySet()) {
            final String finderName = finderNameToKeysOfSecrets.getKey();
            finderNameToKeysOfSecrets.getValue().removeIf(
                    tagName -> spanS3ConfigFetcher.isTagInWhiteList(finderName, serviceName, operationName, tagName));
            if (!finderNameToKeysOfSecrets.getValue().isEmpty() && FINDERS_TO_LOG.contains(finderName)) {
                logger.info(emailText);
                incrementCounter(serviceName, finderName, application);
            }
        }

        return Collections.singleton(emailText);
    }

    @SuppressWarnings("WeakerAccess")
    public static String getEmailText(Span span, Map<String, List<String>> mapOfTypeToKeysOfSecrets) {
        return String.format(TEXT_TEMPLATE, span.getServiceName(), span.getOperationName(), span.getSpanId(),
                span.getTraceId(), mapOfTypeToKeysOfSecrets.toString());
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