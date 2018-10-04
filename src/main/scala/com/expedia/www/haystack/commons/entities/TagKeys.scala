/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.expedia.www.haystack.commons.entities

/*
The Tag keys are according to metrics 2.0 specifications see http://metrics20.org/spec/#tag-keys
 */
object TagKeys {
  val OPERATION_NAME_KEY = "operationName"
  val SERVICE_NAME_KEY = "serviceName"
  val RESULT_KEY = "result"
  val STATS_KEY = "stat"
  val ERROR_KEY = "error"
  val INTERVAL_KEY = "interval"
  val ORG_ID_KEY = "orgId"
}
