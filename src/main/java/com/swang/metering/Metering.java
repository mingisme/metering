package com.swang.metering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;


public class Metering {

    final static Logger logger = LoggerFactory.getLogger(Metering.class);

    public static final int MSG_SIZE = 512;
    public static final Duration REPORT_FREQUENCY = Duration.ofSeconds(3);
    public static final long UPPER_BOUND_UNIT_TIME_MS = REPORT_FREQUENCY.getSeconds() * 1000;
    public static final int PRODUCT_ID = 1102;
    public static final String IOT_METERING = "iot-metering-stream-input";
    public static final String PERIODICAL_REPORT = "periodical-report";
    public static final String IOT_WINDOW_STORE = "agg-window-store";
    public static final String SUPPRESS_WINDOW_STORE = "sup-window";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        Metering metering = new Metering();
        final Properties props = metering.getProperties();
        final Topology topology = metering.getSumTopology();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

    protected Properties getProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "iot-sum-metering");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka9002:9092,kafka9003:9092");
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    protected Topology getSumTopology() {
        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Report> reports = builder.stream(IOT_METERING, Consumed.with(new Serdes.StringSerde(), new JSONSerde<>()));

        KStream<String, Report> fixedReports = reports.mapValues(value -> {
            logger.debug("message input: {}", value);
            long numberByByte = (value.getBytesNumber() + MSG_SIZE - 1) / MSG_SIZE;
            value.setRecordNumber(Math.max(value.getRecordNumber(), numberByByte));
            return value;
        });

        KGroupedStream<String, Report> groupedReports = fixedReports.groupByKey();

        TimeWindowedKStream<String, Report> windowedReports =
                groupedReports.windowedBy(TimeWindows.ofSizeWithNoGrace(REPORT_FREQUENCY));

        KTable<Windowed<String>, Report> aggregatedReports = windowedReports.aggregate(Report::new, (key, value, report) -> {
            report.setOrgId(value.getOrgId());
            report.setProject(value.getProject());
            report.setTimestamp(value.getTimestamp());
            report.setRecordNumber(report.getRecordNumber() + value.getRecordNumber());
            report.setBytesNumber(report.getBytesNumber() + value.getBytesNumber());
            return report;
        }, Materialized.<String, Report, WindowStore<Bytes, byte[]>>as(IOT_WINDOW_STORE)
                .withKeySerde(new Serdes.StringSerde()).withValueSerde(new JSONSerde<>()));

        KTable<Windowed<String>, Report> suppressedReports =
                aggregatedReports.suppress(Suppressed.untilWindowCloses(unbounded()).withName(SUPPRESS_WINDOW_STORE));

        KStream<byte[], String> periodicalReports = suppressedReports.toStream().map((key, value) -> {



            InstanceDTO instanceDTO = new InstanceDTO();
            instanceDTO.setPropertyName(value.getProject());
            instanceDTO.setOuId(value.getOrgId());
            instanceDTO.setUsage(value.getRecordNumber() * 1.0);

            ReportDTO reportDTO = new ReportDTO();
            reportDTO.setProductId(PRODUCT_ID);
            reportDTO.setReportList(new ArrayList<>(Collections.singletonList(instanceDTO)));
            reportDTO.setStatisticTime(new Date(System.currentTimeMillis() / UPPER_BOUND_UNIT_TIME_MS * UPPER_BOUND_UNIT_TIME_MS));

            try {
                String reportDTOStr = OBJECT_MAPPER.writer().writeValueAsString(Collections.singletonList(reportDTO));
                logger.debug("message output: {}", value);
                return new KeyValue<>(null, reportDTOStr);
            } catch (JsonProcessingException e) {
                logger.error("fail to convert reportDTO", e);
                return new KeyValue<>(null, null);
            }
        });

        periodicalReports.to(PERIODICAL_REPORT, Produced.with(new Serdes.ByteArraySerde(), new Serdes.StringSerde()));

        return builder.build();
    }
}
