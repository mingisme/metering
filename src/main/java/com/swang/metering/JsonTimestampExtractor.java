package com.swang.metering;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class JsonTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(final ConsumerRecord<Object, Object> record, final long partitionTime) {
        if (record.value() instanceof Report) {
            return ((Report) record.value()).getTimestamp();
        }

        return System.currentTimeMillis();
    }
}
