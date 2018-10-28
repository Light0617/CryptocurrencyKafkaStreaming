## launch zookeeper in kafka_2.11–2.0.0
~~~
bin/zookeeper-server-start.sh config/zookeeper.properties
~~~

## launch kafka server in kafka_2.11–2.0.0
~~~
bin/kafka-server-start.sh config/server.properties
~~~


## run connect-distributed
~~~
./confluent-5.0.0/bin/connect-distributed connect.properties
~~~


## check Cassandra will in the plugins
~~~
curl http://localhost:8083/connector-plugins
~~~

## launch the producer and consumer to generate topics
~~~
run some JAVA codes
~~~

## launch Cassandra
~~~
source /Users/light0617/.bash_profile
/opt/cassandra/bin/cassandra
~~~

## create a new connector , which will feed the topics into Cassandra
~~~
java -jar kafka-connect-cli-1.0.6-all.jar create cassandra-sink-coins < cassandra-sink-distributed-coins.properties
~~~
