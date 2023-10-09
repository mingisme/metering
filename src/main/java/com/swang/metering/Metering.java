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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
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
    public static final String SUPPRESS_WINDOW = "sup-window";

    public static final String IOT_RPT_WINDOW_STORE = "iot-rpt-window-store";
    public static final String SUP_RPT_WINDOW = "sup-rpt-window";

    public static final int NUM_STREAM_THREAD = 8;
    public static final int GRACE_MULTIPLY_FACTOR = 10;

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
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JSONSerde.class);
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, JsonTimestampExtractor.class);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, NUM_STREAM_THREAD);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    protected Topology getSumTopology() {
        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Report> reports = builder.stream(IOT_METERING, Consumed.with(new Serdes.StringSerde(), new JSONSerde<>()));

        reports.mapValues(value -> {
                    logger.trace("message input: {}", value);
                    long numberByByte = (value.getBytesNumber() + MSG_SIZE - 1) / MSG_SIZE;
                    value.setRecordNumber(Math.max(value.getRecordNumber(), numberByByte));
                    return value;
                })
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(REPORT_FREQUENCY))
                .reduce((value1, value2) -> {
                    value2.setRecordNumber(value1.getRecordNumber() + value2.getRecordNumber());
                    value2.setBytesNumber(value1.getBytesNumber() + value2.getBytesNumber());
                    return value2;
                }, Materialized.<String, Report, WindowStore<Bytes, byte[]>>as(IOT_WINDOW_STORE)
                        .withKeySerde(new Serdes.StringSerde()).withValueSerde(new JSONSerde<>()))
                .suppress(Suppressed.untilWindowCloses(unbounded()).withName(SUPPRESS_WINDOW))
                .toStream().map((key, value) -> {
                    InstanceDTO instanceDTO = new InstanceDTO();
                    instanceDTO.setPropertyName(value.getProject());
                    instanceDTO.setOuId(value.getOrgId());
                    instanceDTO.setUsage(value.getRecordNumber() * 1.0);

                    ReportDTO reportDTO = new ReportDTO();
                    reportDTO.setProductId(PRODUCT_ID);
                    reportDTO.setReportList(new ArrayList<>(Collections.singletonList(instanceDTO)));
                    reportDTO.setStatisticTime(Date.from(key.window().endTime()));

                    String newKey = String.format("%s", reportDTO.getStatisticTime().getTime());
                    logger.trace("aggregated message: {}, {}",
                            newKey,
                            instanceDTO);
                    return new KeyValue<>(newKey, reportDTO);
                })
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeAndGrace(REPORT_FREQUENCY, REPORT_FREQUENCY.multipliedBy(GRACE_MULTIPLY_FACTOR)))
                .reduce((value1, value2) -> {
                    value2.getReportList().addAll(value1.getReportList());
                    return value2;
                }, Materialized.<String, ReportDTO, WindowStore<Bytes, byte[]>>as(IOT_RPT_WINDOW_STORE)
                        .withKeySerde(new Serdes.StringSerde())
                        .withValueSerde(new JSONSerde<>()))
                .suppress(Suppressed.untilWindowCloses(unbounded()).withName(SUP_RPT_WINDOW))
                .toStream().map((key, value) -> {
                    try {
                        String newValue = OBJECT_MAPPER.writer().writeValueAsString(Collections.singletonList(value));
                        logger.debug("message output: {}", newValue);
                        return new KeyValue<>((byte[]) null, newValue);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .to(PERIODICAL_REPORT, Produced.with(new Serdes.ByteArraySerde(), new Serdes.StringSerde()));

        return builder.build();
    }
}
