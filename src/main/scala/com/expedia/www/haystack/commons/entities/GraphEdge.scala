/*
 *
 *     Copyright 2018 Expedia, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *
 */
package com.expedia.www.haystack.commons.entities

import java.util


/**
  * Case class with enough information to build a relationship between two service graph nodes
  *
  * @param source      identifier for the source graph node
  * @param destination identifier for the destination graph node
  * @param operation   identifier for the graph edge
  * @param tags contains list of tags that can be associated with a edge. Tag would provide additional information
  *             regarding a edge. (Example: service tier indicating the importance of the service)
  */
case class GraphEdge(source: String, destination: String, operation: String, tags: util.Map[String, String]
= new util.HashMap[String, String]()) {
}

