package com.swang.metering;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonTimestampExtractor implements TimestampExtractor {

    private static final Logger logger = LoggerFactory.getLogger(JsonTimestampExtractor.class);

    @Override
    public long extract(final ConsumerRecord<Object, Object> record, final long partitionTime) {
        if (record.value() instanceof ReportDTO) {
            logger.info("extract timestamp of ReportDTO");
            return ((ReportDTO) record.value()).getStatisticTime().getTime();
        }

        return System.currentTimeMillis();
    }
}
