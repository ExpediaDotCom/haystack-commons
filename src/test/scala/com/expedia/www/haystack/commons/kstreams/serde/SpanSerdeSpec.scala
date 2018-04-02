package com.expedia.www.haystack.commons.kstreams.serde

import java.util.UUID

import com.expedia.open.tracing.{Log, Span, Tag}
import com.expedia.www.haystack.commons.unit.UnitTestSpec

class SpanSerdeSpec extends UnitTestSpec {
  val SERVER_SEND_EVENT = "ss"
  val SERVER_RECV_EVENT = "sr"
  val CLIENT_SEND_EVENT = "cs"
  val CLIENT_RECV_EVENT = "cr"

  "span serializer" should {
    "should serialize a span" in {
      Given("a span serializer")
      val serializer = (new SpanSerde).serializer
      And("a valid span is provided")
      val span = newSpan("foo", "bar", 100, client = true, server = false)
      When("span serializer is used to serialize the span")
      val bytes = serializer.serialize("proto-spans", span)
      Then("it should serialize the object")
      bytes.nonEmpty should be(true)
    }
  }
  "span deserializer" should {
    "should deserialize a span" in {
      Given("a span deserializer")
      val serializer = (new SpanSerde).serializer
      val deserializer = (new SpanSerde).deserializer
      And("a valid span is provided")
      val span = newSpan("foo", "bar", 100, client = true, server = false)
      When("span deserializer is used on valid array of bytes")
      val bytes = serializer.serialize("proto-spans", span)
      val span2 = deserializer.deserialize("proto-spans", bytes)
      Then("it should deserialize correctly")
      span should be(span2)
    }
  }

  private def newSpan(serviceName: String, operation: String, duration: Long, client: Boolean, server: Boolean): Span = {
    newSpan(UUID.randomUUID().toString, serviceName, operation, duration, client, server)
  }

  private def newSpan(spanId: String, serviceName: String, operation: String, duration: Long, client: Boolean, server: Boolean): Span = {
    val ts = System.currentTimeMillis() - (10 * 1000)
    newSpan(spanId, ts, serviceName, operation, duration, client, server)
  }

  private def newSpan(spanId: String, ts: Long, serviceName: String, operation: String, duration: Long, client: Boolean, server: Boolean): Span = {


    val spanBuilder = Span.newBuilder()
    spanBuilder.setTraceId(UUID.randomUUID().toString)
    spanBuilder.setSpanId(spanId)
    spanBuilder.setServiceName(serviceName)
    spanBuilder.setOperationName(operation)
    spanBuilder.setStartTime(ts)
    spanBuilder.setDuration(duration)

    val logBuilder = Log.newBuilder()
    if (client) {
      logBuilder.setTimestamp(ts)
      logBuilder.addFields(Tag.newBuilder().setKey("event").setVStr(CLIENT_SEND_EVENT).build())
      spanBuilder.addLogs(logBuilder.build())
      logBuilder.clear()
      logBuilder.setTimestamp(ts + duration)
      logBuilder.addFields(Tag.newBuilder().setKey("event").setVStr(CLIENT_RECV_EVENT).build())
      spanBuilder.addLogs(logBuilder.build())
    }

    if (server) {
      logBuilder.setTimestamp(ts)
      logBuilder.addFields(Tag.newBuilder().setKey("event").setVStr(SERVER_RECV_EVENT).build())
      spanBuilder.addLogs(logBuilder.build())
      logBuilder.clear()
      logBuilder.setTimestamp(ts + duration)
      logBuilder.addFields(Tag.newBuilder().setKey("event").setVStr(SERVER_SEND_EVENT).build())
      spanBuilder.addLogs(logBuilder.build())
    }

    spanBuilder.build()
  }

}
