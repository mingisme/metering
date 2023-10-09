# metering


# Ceate topic
## 1.input topic
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic iot-metering-stream-input  --replication-factor 3 --partitions 8

## 2.window-aggregate-topic
   bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic iot-sum-metering-agg-window-store-changelog --replication-factor 3 --partitions 8 --config cleanup.policy=compact

## 3.window-suppress-topic
   bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic iot-sum-metering-sup-window-store-changelog --replication-factor 3 --partitions 8 --config cleanup.policy=compact

##  4.time product repartition topic
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic iot-sum-metering-iot-rpt-window-store-repartition --replication-factor 3 --partitions 8 --config cleanup.policy=compact

##  5.report window aggregate topic
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic iot-sum-metering-iot-rpt-window-store-changelog --replication-factor 3 --partitions 8 --config cleanup.policy=compact

##  6.report window supress topic
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic iot-sum-metering-sup-rpt-window-store-changelog --replication-factor 3 --partitions 8 --config cleanup.policy=compact


