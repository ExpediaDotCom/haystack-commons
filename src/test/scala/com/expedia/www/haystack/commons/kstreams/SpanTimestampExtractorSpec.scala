package com.expedia.www.haystack.commons.kstreams

import java.util.UUID

import com.expedia.www.haystack.commons.unit.UnitTestSpec
import org.apache.kafka.clients.consumer.ConsumerRecord

class SpanTimestampExtractorSpec extends UnitTestSpec {

  "SpanTimestampExtractor" should {

    " should extract timestamp from Span" in {

      Given("a span with some timestamp")
      val currentTimeInMicroSeconds = System.currentTimeMillis() * 1000

      val span = generateTestSpan(UUID.randomUUID().toString, currentTimeInMicroSeconds, "foo", "bar", 20, client = false, server = true)
      val spanTimestampExtractor = new SpanTimestampExtractor
      val record: ConsumerRecord[AnyRef, AnyRef] = new ConsumerRecord("dummy-topic", 1, 1, "dummy-key", span)

      When("extract timestamp")
      val epochTime = spanTimestampExtractor.extract(record, System.currentTimeMillis())

      Then("extracted time should equal span startTime time")
      epochTime shouldEqual currentTimeInMicroSeconds / 1000
    }
  }
}
