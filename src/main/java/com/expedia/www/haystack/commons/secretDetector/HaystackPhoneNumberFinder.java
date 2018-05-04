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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import io.dataapps.chlorine.finder.Finder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class HaystackPhoneNumberFinder implements Finder {
    private static final String FINDER_NAME = "PhoneNumber";
    private static final String REGION = "US";
    private static final Pattern ALPHAS_PATTERN = Pattern.compile("[A-Za-z]+");
    private static final Pattern ALL_NUMBERS_PATTERN = Pattern.compile("^\\d+$");
    private final PhoneNumberUtil phoneNumberUtil;

    public HaystackPhoneNumberFinder() {
        this(PhoneNumberUtil.getInstance())   ;
    }

    public HaystackPhoneNumberFinder(PhoneNumberUtil phoneNumberUtil) {
        this.phoneNumberUtil = phoneNumberUtil;
    }

    @Override
    public String getName() {
        return FINDER_NAME;
    }

    @Override
    public List<String> find(Collection<String> inputs) {
        final List<String> list = new ArrayList<>();
        for(String input : inputs) {
            list.addAll(find(input));
        }
        return list;
    }

    @Override
    public List<String> find(String input) {
        try {
            if(!containsAnyAlphabeticCharacters(input) && isNotIpV4Address(input) && !containsOnlyNumbers(input)) {
                final PhoneNumber phoneNumber = phoneNumberUtil.parseAndKeepRawInput(input, REGION);
                if (phoneNumberUtil.isValidNumberForRegion(phoneNumber, REGION)) {
                    return Collections.singletonList(phoneNumber.getRawInput());
                }
            }
        } catch (NumberParseException e) {
            // Just ignore, it's not a phone number
        }
        return Collections.emptyList();
    }

    private boolean containsAnyAlphabeticCharacters(String input) {
        final Matcher matcher = ALPHAS_PATTERN.matcher(input);
        return matcher.find();
    }

    private boolean isNotIpV4Address(String input) {
        return NonLocalIpV4AddressFinder.IPV4_FINDER.find(input).isEmpty();
    }

    private boolean containsOnlyNumbers(String input) {
        return ALL_NUMBERS_PATTERN.matcher(input).find();
    }
}
