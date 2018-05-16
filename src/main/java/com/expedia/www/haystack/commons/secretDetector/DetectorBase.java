package com.expedia.www.haystack.commons.secretDetector;

import io.dataapps.chlorine.finder.FinderEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class DetectorBase {
    final FinderEngine finderEngine;
    final SpanS3ConfigFetcher spanS3ConfigFetcher;

    DetectorBase(FinderEngine finderEngine, SpanS3ConfigFetcher spanS3ConfigFetcher) {
        this.finderEngine = finderEngine;
        this.spanS3ConfigFetcher = spanS3ConfigFetcher;
    }

    void putKeysOfSecretsIntoMap(Map<String, List<String>> mapOfTypeToKeysOfSecrets,
                                 String key,
                                 Map<String, List<String>> mapOfTypeToKeysOfSecretsJustFound) {
        for (final String finderName : mapOfTypeToKeysOfSecretsJustFound.keySet()) {
            mapOfTypeToKeysOfSecrets.computeIfAbsent(finderName, (l -> new ArrayList<>())).add(key);
        }
    }


}
