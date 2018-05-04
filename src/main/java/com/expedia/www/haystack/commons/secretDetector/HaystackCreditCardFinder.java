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

import io.dataapps.chlorine.pattern.CreditCardFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Extends chlorine-finder's CreditCardFinder by assuming that a credit card comes in with a single value in the string.
 */
public class HaystackCreditCardFinder extends CreditCardFinder {
    @SuppressWarnings("WeakerAccess")
    public HaystackCreditCardFinder(String name, String pattern) {
        super(name, pattern);
    }

    @Override
    public List<String> find(String sample) {
        final List<String> matches = new ArrayList<>();
        final Matcher matcher = getPattern().matcher(sample);
        while (matcher.find()) {
            if (postMatchCheck(sample)) {
                matches.add(sample);
            }
        }
        return matches;
    }
}
