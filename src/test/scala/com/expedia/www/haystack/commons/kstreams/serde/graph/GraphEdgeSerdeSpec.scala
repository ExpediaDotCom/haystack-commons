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

package com.expedia.www.haystack.commons.kstreams.serde.graph

import com.expedia.www.haystack.commons.entities.{GraphEdge, TagKeys}
import com.expedia.www.haystack.commons.unit.UnitTestSpec

import scala.collection.JavaConverters._

class GraphEdgeSerdeSpec extends UnitTestSpec {
  "GraphEdge serializer" should {
    "should serialize a GraphEdge" in {
      Given("a GraphEdge serializer")
      val serializer = (new GraphEdgeSerde).serializer()

      And("a valid GraphEdge is provided")
      val edge = GraphEdge("sourceSvc", "destinationSvc", "operation", Map("X-HAYSTACK-INFRASTRUCTURE-PROVIDER" -> "aws",
        "tier" -> "1").asJava)

      When("GraphEdge serializer is used to serialize the GraphEdge")
      val bytes = serializer.serialize("graph-nodes", edge)

      Then("it should serialize the object")
      bytes.nonEmpty should be(true)
    }
  }

  "GraphEdge deserializer" should {
    "should deserialize a GraphEdge" in {
      Given("a GraphEdge deserializer")
      val serializer = (new GraphEdgeSerde).serializer()
      val deserializer = (new GraphEdgeSerde).deserializer()

      And("a valid GraphEdge is provided")
      val edge = GraphEdge("sourceSvc", "destinationSvc", "operation", Map("X-HAYSTACK-INFRASTRUCTURE-PROVIDER" -> "aws",
        "tier" -> "1").asJava)

      When("GraphEdge deserializer is used on valid array of bytes")
      val bytes = serializer.serialize("graph-nodes", edge)
      val serializedEdge = deserializer.deserialize("graph-nodes", bytes)

      Then("it should deserialize correctly")
      edge should be(serializedEdge)
    }
  }
}
