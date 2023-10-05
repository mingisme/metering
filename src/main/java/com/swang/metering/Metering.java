/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.swang.metering;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;


public class Metering {

    final static Logger logger = LoggerFactory.getLogger(Metering.class);


    public static final int MSG_SIZE = 512;
    public static final Duration REPORT_FREQUENCY = Duration.ofSeconds(5);
    public static final String ORIGIN_REPORT = "origin-report";
    public static final String PERIODICAL_REPORT = "periodical-report";
    public static final String METERING_STORE = "metering-store";


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
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "metering");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka9002:9092,kafka9003:9092");
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, JsonTimestampExtractor.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    protected Topology getSumTopology() {
        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Report> reports = builder.stream(ORIGIN_REPORT, Consumed.with(new Serdes.StringSerde(),new JSONSerde<>()));

        KStream<String, Report> fixedReports = reports.mapValues(value -> {
            long numberByByte = (value.getBytes() + MSG_SIZE - 1) / MSG_SIZE;
            if (numberByByte > value.getNumber()) {
                value.setNumber(numberByByte);
            }
            return value;
        });

        KGroupedStream<String, Report> groupedReports = fixedReports.groupByKey();

        TimeWindowedKStream<String, Report> windowedReports =
                groupedReports.windowedBy(TimeWindows.of(REPORT_FREQUENCY).grace(REPORT_FREQUENCY.dividedBy(5)));

        KTable<Windowed<String>, Report> aggregatedReports = windowedReports.reduce((value1, value2) -> {
            value1.setNumber(value1.getNumber() + value2.getNumber());
            return value1;
        },  Materialized.<String, Report, WindowStore<Bytes, byte[]>>as(METERING_STORE)
                .withKeySerde(new Serdes.StringSerde()).withValueSerde(new JSONSerde<>()));

        KTable<Windowed<String>, Report> suppressedReports = aggregatedReports.suppress(Suppressed.untilWindowCloses(unbounded()));

        KStream<String, Report> periodicalReports = suppressedReports.toStream().map((key, value) -> {
            value.setTimestamp(System.currentTimeMillis());
            return new KeyValue<>(key.key(), value);
        });

        periodicalReports.to(PERIODICAL_REPORT, Produced.with(new Serdes.StringSerde(),new JSONSerde<>()));

        return builder.build();
    }
}
