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

import io.dataapps.chlorine.finder.CompositeFinder;
import io.dataapps.chlorine.finder.Finder;

import java.util.Collection;
import java.util.List;

@SuppressWarnings({"SameParameterValue"})
public class HaystackCompositeCreditCardFinder implements Finder {
    private static final String PERCENT_S = "%s";
    private static final String GROUP_SEPARATOR = "(\\ |\\-|)"; // space or dash or nothing
    private static final String THREE_GROUP_PATTERN = String.format("^%s%s%s%s%s$",
            PERCENT_S, GROUP_SEPARATOR, PERCENT_S, GROUP_SEPARATOR, PERCENT_S);
    private static final String FOUR_GROUP_PATTERN = String.format("^%s%s%s%s%s%s%s$",
            PERCENT_S, GROUP_SEPARATOR, PERCENT_S, GROUP_SEPARATOR, PERCENT_S, GROUP_SEPARATOR, PERCENT_S);
    private static final String FOUR_DIGITS = "[0-9]{4}";
    private static final String FIVE_DIGITS = "[0-9]{5}";
    private static final String SIX_DIGITS = "[0-9]{6}";
    private static final String MASTERCARD_PREFIX = "(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)";

    static final String MASTERCARD_PATTERN = build4(MASTERCARD_PREFIX, FOUR_DIGITS, FOUR_DIGITS, FOUR_DIGITS);
	static final String VISA_PATTERN = build4("4[0-9]{3}", FOUR_DIGITS, FOUR_DIGITS, FOUR_DIGITS);
    static final String AMEX_PATTERN = build3("(34|37)[0-9]{2}", SIX_DIGITS, FIVE_DIGITS);
	static final String DINERS_CLUB_1_PATTERN = build3("30[0-5][0-9]", SIX_DIGITS, FOUR_DIGITS);
	static final String DINERS_CLUB_2_PATTERN = build3("(36|38)[0-9]{2}", SIX_DIGITS, FOUR_DIGITS);
	static final String DISCOVER_PATTERN = build4("6011", FOUR_DIGITS, FOUR_DIGITS, FOUR_DIGITS);
    static final String JCB_1_PATTERN = build4("3[0-9]{3}", FOUR_DIGITS, FOUR_DIGITS, FOUR_DIGITS);
	static final String JCB_2_PATTERN = "^(2131|1800)[0-9]{11}$";

	private static String build3(String s1, String s2, String s3) {
	    return String.format(THREE_GROUP_PATTERN, s1, s2, s3);
    }
	private static String build4(String s1, String s2, String s3, String s4) {
	    return String.format(FOUR_GROUP_PATTERN, s1, s2, s3, s4);
    }
	private final CompositeFinder compositeFinder;

	HaystackCompositeCreditCardFinder(CompositeFinder compositeFinder) {
	    this.compositeFinder = compositeFinder;
    }
	/**
	 * Like the CompositeCreditCardFinder provided by chlorine-finder, but with a regular expression that insists
	 * that the value only matches if it is the entire tag value.
	 */
    @SuppressWarnings("WeakerAccess")
	public HaystackCompositeCreditCardFinder() {
	    this(new CompositeFinder("Credit_Card"));
		compositeFinder.add(new HaystackCreditCardFinder("Mastercard", MASTERCARD_PATTERN));
		compositeFinder.add(new HaystackCreditCardFinder("Visa", VISA_PATTERN));
		compositeFinder.add(new HaystackCreditCardFinder("AMEX", AMEX_PATTERN));
		compositeFinder.add(new HaystackCreditCardFinder("Diners Club 1", DINERS_CLUB_1_PATTERN));
		compositeFinder.add(new HaystackCreditCardFinder("Diners Club 2", DINERS_CLUB_2_PATTERN));
		compositeFinder.add(new HaystackCreditCardFinder("Discover", DISCOVER_PATTERN));
		compositeFinder.add(new HaystackCreditCardFinder("JCB 1", JCB_1_PATTERN));
		compositeFinder.add(new HaystackCreditCardFinder("JCB 2", JCB_2_PATTERN));
	}

	@Override
	public List<String> find(Collection<String> samples) {
		return compositeFinder.find(samples);
	}

	@Override
	public List<String> find(String sample) {
		return compositeFinder.find(sample);
	}

	@Override
	public String getName() {
		return compositeFinder.getName();
	}

}
