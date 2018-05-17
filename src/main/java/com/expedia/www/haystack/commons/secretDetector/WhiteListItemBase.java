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

import java.util.Arrays;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class WhiteListItemBase {
    protected final String[] items;

    protected WhiteListItemBase(String... items) {
        this.items = items.clone();
    }

    @SuppressWarnings({"ParameterNameDiffersFromOverriddenParameter", "UnclearExpression", "QuestionableName", "AccessingNonPublicFieldOfAnotherObject"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhiteListItemBase that = (WhiteListItemBase) o;
        return Arrays.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }
}
