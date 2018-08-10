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

import com.expedia.www.haystack.commons.entities.{MetricPoint, MetricType}
import com.expedia.www.haystack.commons.unit.UnitTestSpec
import org.apache.kafka.clients.consumer.ConsumerRecord

class MetricPointTimestampExtractorSpec extends UnitTestSpec {

  "MetricPointTimestampExtractor" should {

    "extract timestamp from MetricPoint" in {

      Given("a metric point with some timestamp")
      val currentTimeInSecs = computeCurrentTimeInSecs
      val metricPoint = MetricPoint("duration", MetricType.Gauge, null, 80, currentTimeInSecs)
      val metricPointTimestampExtractor = new MetricPointTimestampExtractor
      val record: ConsumerRecord[AnyRef, AnyRef] = new ConsumerRecord("dummy-topic", 1, 1, "dummy-key", metricPoint)

      When("extract timestamp")
      val epochTime = metricPointTimestampExtractor.extract(record, System.currentTimeMillis())

      Then("extracted time should equal metric point time in milliseconds")
      epochTime shouldEqual currentTimeInSecs * 1000
    }
  }
}
