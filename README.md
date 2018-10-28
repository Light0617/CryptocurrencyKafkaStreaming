## launch zookeeper in kafka_2.11–2.0.0
~~~
bin/zookeeper-server-start.sh config/zookeeper.properties
~~~

## launch kafka server in kafka_2.11–2.0.0
~~~
bin/kafka-server-start.sh config/server.properties
~~~


## launch Cassandra
~~~
source /Users/light0617/.bash_profile
export CASSANDRA_HOME=/opt/cassandra
/opt/cassandra/bin/cassandra
cqlsh
~~~

## launch the producer and consumer to generate topics
~~~
run some JAVA codes
~~~

## check the result in Cassandra

~~~
cqlsh
~~~