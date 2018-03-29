package com.expedia.www.haystack.commons.kstreams

import com.expedia.www.haystack.commons.entities.MetricPoint
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.processor.TimestampExtractor

class MetricPointTimestampExtractor extends TimestampExtractor {

  override def extract(record: ConsumerRecord[AnyRef, AnyRef], previousTimestamp: Long): Long = {

    //The startTime for metricpoints in computed in seconds and hence multiplying by 1000 to create the epochTimeInMs
    record.value().asInstanceOf[MetricPoint].epochTimeInSeconds * 1000

  }
}
