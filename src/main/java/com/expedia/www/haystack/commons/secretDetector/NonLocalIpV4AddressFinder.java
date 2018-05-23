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

import io.dataapps.chlorine.finder.Finder;
import io.dataapps.chlorine.pattern.RegexFinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Finds IP V4 addresses, but ignores those in the 10.0.0.0/8, 192.168.0.0/16, and 127.0.0.0/8 ranges.
 */
public class NonLocalIpV4AddressFinder implements Finder {
    static final Finder IPV4_FINDER = new RegexFinder("IPV4", "(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");

    private final Pattern pattern10Dot = Pattern.compile("(^10\\.)");
    private final Pattern pattern192Dot168 = Pattern.compile("(^192\\.168\\.)");
    private final Pattern pattern127Dot0Dot0 = Pattern.compile("(^127\\.0\\.0\\.)");

    @Override
    public String getName() {
        return "NonLocalIpV4Address";
    }

    @Override
    public List<String> find(Collection<String> inputs) {
        final List<String> strings = new ArrayList<>();
        for (String input : inputs) {
            strings.addAll(find(input));
        }
        return strings;
    }

    @Override
    public List<String> find(String input) {
        final List<String> strings = IPV4_FINDER.find(input);
        final Iterator<String> iterator = strings.iterator();
        while (iterator.hasNext()) {
            final String ipAddressFromIpv4Finder = iterator.next();
            final Matcher matcher10Dot = pattern10Dot.matcher(ipAddressFromIpv4Finder);
            if (matcher10Dot.find()) {
                iterator.remove();
            } else {
                final Matcher matcher192Dot168 = pattern192Dot168.matcher(ipAddressFromIpv4Finder);
                if (matcher192Dot168.find()) {
                    iterator.remove();
                } else {
                    final Matcher matcher127Dot0Dot0 = pattern127Dot0Dot0.matcher(ipAddressFromIpv4Finder);
                    if (matcher127Dot0Dot0.find()) {
                        iterator.remove();
                    }
                }
            }
        }
        return strings;
    }
}
