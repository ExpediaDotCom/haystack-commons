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
package com.expedia.www.haystack.commons.secretDetector.span;

import com.expedia.www.haystack.commons.secretDetector.WhiteListItemBase;

class SpanWhiteListItem extends WhiteListItemBase {

    SpanWhiteListItem(String finderName, String serviceName, String operationName, String tagName) {
        super(finderName, serviceName, operationName, tagName);
    }

    String getFinderName() {
        return items[0];
    }

    String getServiceName() {
        return items[1];
    }

    String getOperationName() {
        return items[2];
    }

    String getTagName() {
        return items[3];
    }
}
