package com.expedia.www.haystack.commons.kstreams

import com.expedia.www.haystack.commons.entities.{MetricPoint, MetricType}
import com.expedia.www.haystack.commons.unit.UnitTestSpec
import org.apache.kafka.clients.consumer.ConsumerRecord

class MetricPointTimestampExtractorSpec extends UnitTestSpec {

  "MetricPointTimestampExtractor" should {

    "should extract timestamp from MetricPoint" in {

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
