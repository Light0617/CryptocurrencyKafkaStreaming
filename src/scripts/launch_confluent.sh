#!/usr/bin/env bash

#launch confluent
cd /opt/confluent-5.0.0
bin/confluent start


bin/connect-distributed etc/schema-registry/

# check topics list
./bin/kafka-topics --list --zookeeper localhost:2181

# create three topics
./bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic coins
./bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic coins-price-info --config cleanup.policy=compact
./bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic coins-volatility --config cleanup.policy=compact

# launch a Kafka consumer
./bin/kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic coins-price-info \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer

./bin/kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic coins-volatility \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
