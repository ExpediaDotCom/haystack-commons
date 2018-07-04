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

import com.expedia.www.haystack.commons.entities.{GraphEdge, GraphVertex}
import com.expedia.www.haystack.commons.unit.UnitTestSpec

import scala.collection.JavaConverters._

class GraphEdgeKeySerdeSpec extends UnitTestSpec {
  "GraphEdge Key serializer" should {
    "should serialize a GraphEdge" in {
      Given("a GraphEdge serializer")
      val serializer = (new GraphEdgeKeySerde).serializer()

      And("a valid GraphEdge is provided")
      val edge = GraphEdge(GraphVertex("sourceSvc"), GraphVertex("destinationSvc"),
        "operation")

      When("GraphEdge serializer is used to serialize the GraphEdge")
      val bytes = serializer.serialize("graph-nodes", edge)

      Then("it should serialize the object")
      new String(bytes) shouldEqual "{\"source\":{\"name\":\"sourceSvc\",\"tags\":{}},\"destination\":{\"name\":\"destinationSvc\",\"tags\":{}},\"operation\":\"operation\"}"
    }
  }

  "GraphEdge Key deserializer" should {
    "should deserialize a GraphEdge" in {
      Given("a GraphEdge deserializer")
      val serializer = (new GraphEdgeKeySerde).serializer()
      val deserializer = (new GraphEdgeKeySerde).deserializer()

      And("a valid GraphEdge is provided")
      val edge = GraphEdge(GraphVertex("sourceSvc", Map("testtag" -> "true").asJava), GraphVertex("destinationSvc", Map("othertag" -> "true").asJava),
        "operation")

      When("GraphEdge deserializer is used on valid array of bytes")
      val bytes = serializer.serialize("graph-nodes", edge)
      val serializedEdge = deserializer.deserialize("graph-nodes", bytes)

      Then("it should deserialize correctly")
      serializedEdge.source.name should be("sourceSvc")
      serializedEdge.destination.name should be("destinationSvc")
      serializedEdge.source.tags.size() shouldBe 0
      serializedEdge.destination.tags.size() shouldBe 0
    }
  }
}
