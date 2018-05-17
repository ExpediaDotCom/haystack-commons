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

import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class FinderNameAndServiceName {
    private final String finderName;
    private final String serviceName;

    public FinderNameAndServiceName(String finderName, String serviceName) {
        this.finderName = finderName;
        this.serviceName = serviceName;
    }

    public String getFinderName() {
        return finderName;
    }

    public String getServiceName() {
        return serviceName;
    }

    // equals and hashCode are overridden with this IDE-created code so that FinderNameAndServiceName objects can
    // be the key in the static SpanDetector.COUNTERS object.
    @SuppressWarnings({"UnclearExpression", "QuestionableName", "AccessingNonPublicFieldOfAnotherObject", "ParameterNameDiffersFromOverriddenParameter"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FinderNameAndServiceName that = (FinderNameAndServiceName) o;
        return Objects.equals(finderName, that.finderName) &&
                Objects.equals(serviceName, that.serviceName);
    }

    @SuppressWarnings("ObjectInstantiationInEqualsHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(finderName, serviceName);
    }
}
