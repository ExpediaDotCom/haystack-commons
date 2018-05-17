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

import io.dataapps.chlorine.finder.FinderEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class DetectorBase {
    protected final FinderEngine finderEngine;
    protected final S3ConfigFetcherBase s3ConfigFetcher;

    protected DetectorBase(FinderEngine finderEngine, S3ConfigFetcherBase s3ConfigFetcher) {
        this.finderEngine = finderEngine;
        this.s3ConfigFetcher = s3ConfigFetcher;
    }

    protected static void putKeysOfSecretsIntoMap(Map<String, List<String>> mapOfTypeToKeysOfSecrets,
                                                  String key,
                                                  Map<String, List<String>> mapOfTypeToKeysOfSecretsJustFound) {
        for (final String finderName : mapOfTypeToKeysOfSecretsJustFound.keySet()) {
            //noinspection ObjectAllocationInLoop
            mapOfTypeToKeysOfSecrets.computeIfAbsent(finderName, (list -> new ArrayList<>())).add(key);
        }
    }


}
