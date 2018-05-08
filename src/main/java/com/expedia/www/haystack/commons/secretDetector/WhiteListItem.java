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
public class WhiteListItem {
    public final String finderName;
    public final String serviceName;
    public final String operationName;
    public final String tagName;

    public WhiteListItem(String finderName, String serviceName, String operationName, String tagName) {
        this.finderName = finderName;
        this.serviceName = serviceName;
        this.operationName = operationName;
        this.tagName = tagName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhiteListItem that = (WhiteListItem) o;
        return Objects.equals(finderName, that.finderName) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(operationName, that.operationName) &&
                Objects.equals(tagName, that.tagName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(finderName, serviceName, operationName, tagName);
    }
}
