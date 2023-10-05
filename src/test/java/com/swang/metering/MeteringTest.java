package com.swang.metering;


import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.metrics.stats.Meter;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.*;
//import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Properties;


public class MeteringTest {

    @Test
    public void testSumTopology() throws InterruptedException {
        Metering metering = new Metering();
        Topology topology = metering.getSumTopology();
        Properties properties = metering.getProperties();
        TopologyTestDriver driver = new TopologyTestDriver(topology, properties);
        Serializer<String> keySerializer = new Serdes.StringSerde().serializer();
        Serializer valueSserializer = new JSONSerde().serializer();
        Deserializer<String> keyDeserializer = new Serdes.StringSerde().deserializer();
        Deserializer valueDeserializer = new JSONSerde().deserializer();


//        ConsumerRecordFactory factory = new ConsumerRecordFactory(keySerializer, valueSserializer);
//        driver.pipeInput(factory.create(Metering.ORIGIN_REPORT, report.getReportKey(), report));
//        driver.advanceWallClockTime(60*1000);
//        ProducerRecord<String, Report> record1 = driver.readOutput(Metering.PERIODICAL_REPORT, keyDeserializer, valueDeserializer);
//        System.out.println(record1);

        TestInputTopic inputTopic = driver.createInputTopic(Metering.ORIGIN_REPORT, keySerializer, valueSserializer);

        Report report = new Report("o1", "MQTT", System.currentTimeMillis(), 1L, 100L);
        inputTopic.pipeInput(report.getReportKey(), report);
        driver.advanceWallClockTime(Duration.ofSeconds(10));

        WindowStore windowStore = driver.getTimestampedWindowStore(Metering.METERING_STORE);
        KeyValueIterator<Windowed<Object>, Object> all = windowStore.all();
        while (all.hasNext()) {
            System.out.println(all.next());
        }

        TestOutputTopic outputTopic = driver.createOutputTopic(Metering.PERIODICAL_REPORT, keyDeserializer, valueDeserializer);

        while (!outputTopic.isEmpty()) {
            KeyValue keyValue = outputTopic.readKeyValue();
            System.out.println(keyValue);
        }

        driver.close();
    }


}