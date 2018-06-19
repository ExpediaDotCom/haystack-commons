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

import com.expedia.open.tracing.Span

import scala.collection.JavaConverters._
import scala.collection.mutable


/**
  * Define tag names that should be collected when building a GraphEdge.
  */
class GraphEdgeTagCollector(tags: Set[String]) {

  /**
    *
    * @param span: Span containing all the tags
    * @return Filtered list of tag keys and values that match the defined tag names.
    */
  def collectTags(span: Span): Map[String, String] = {
    val edgeTags =  mutable.Map[String, String]()
    span.getTagsList.asScala.filter(t => tags.contains(t.getKey)).foreach { tag =>
      edgeTags += (tag.getKey -> tag.getVStr)
    }
    edgeTags.toMap
  }
}


