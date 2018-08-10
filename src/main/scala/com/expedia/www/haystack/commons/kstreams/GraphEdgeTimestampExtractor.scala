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

package com.expedia.www.haystack.commons.kstreams

import com.expedia.www.haystack.commons.entities.GraphEdge
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.processor.TimestampExtractor


class GraphEdgeTimestampExtractor extends TimestampExtractor {
  override def extract(consumerRecord: ConsumerRecord[AnyRef, AnyRef], previousTimestamp: Long): Long = {
    // sourceTimestamp of GraphEdge in millis
    consumerRecord.value().asInstanceOf[GraphEdge].sourceTimestamp
  }
}