#!/usr/bin/env bash

./bin/kafka-avro-console-producer \
     --broker-list localhost:9092 --topic coins-volatility \
     --property value.schema='{"type":"record","name":"myrecord","fields":[{"name":"id","type":"string"}, {"name":"created", "type": "string"}, {"name":"volatility", "type": "float"}]}'
