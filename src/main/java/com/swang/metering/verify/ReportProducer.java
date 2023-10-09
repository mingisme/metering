package com.swang.metering.verify;

import com.swang.metering.Metering;
import com.swang.metering.Report;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ReportProducer {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", "kafka9002:9092,kafka9003:9092");

        kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer", "com.swang.metering.JSONSerde");

        KafkaProducer<String, Report> producer = new KafkaProducer<>(kafkaProps);

        for (int i = 0; i < 10000; i++) {
            Report report = new Report("orgId-"+i%2, "M", System.currentTimeMillis(), 1L, 512L);
            ProducerRecord<String, Report> record = new ProducerRecord<>(Metering.IOT_METERING, report.getReportKey(), report);
            Future<RecordMetadata> producerFuture = producer.send(record);
            RecordMetadata recordMetadata = producerFuture.get();
            System.out.println(i + ":" + recordMetadata);
            Thread.sleep(1000);
        }

//        Thread.sleep(62000);
//
//        for (int i = 0; i < 10; i++) {
//            Report report = new Report("o" + i % 2, "Messages(Old)2", System.currentTimeMillis(), 1L, 520L);
//            ProducerRecord<String, Report> record =
//                    new ProducerRecord<>(Metering.IOT_METERING, report.getReportKey(),
//                            report);
//            Future<RecordMetadata> producerFuture = producer.send(record);
//            RecordMetadata recordMetadata = producerFuture.get();
//            System.out.println(recordMetadata);
//        }

    }


}
