package com.expedia.www.haystack.commons.kstreams

import com.expedia.open.tracing.Span
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.processor.TimestampExtractor

class SpanTimestampExtractor extends TimestampExtractor {



  override def extract(record: ConsumerRecord[AnyRef, AnyRef], previousTimestamp: Long): Long = {

    //The startTime for span in computed in microseconds and hence dividing by 1000 to create the epochTimeInMs
    record.value().asInstanceOf[Span].getStartTime / 1000

  }
}
