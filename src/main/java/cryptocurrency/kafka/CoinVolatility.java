package cryptocurrency.kafka;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.StreamsConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import java.util.Properties;

public class CoinVolatility {
    private Session session;
    private CassandraConnector client;
    private void setUpSession() {
        client = new CassandraConnector();
        client.connect("127.0.0.1", 9042);
        session = client.getSession();
    }
    private static Properties settingConfig() {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "bank-balance-application");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // we disable the cache to demonstrate all the "steps" involved in the transformation - not recommended in prod
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");
        // Exactly once processing
        config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);
        return config;
    }

    private static Serde<JsonNode> getJsonSerde() {
        final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
        final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
        return Serdes.serdeFrom(jsonSerializer, jsonDeserializer);
    }

    public static void main(String[] args) {
        CassandraConnector client = new CassandraConnector();
        client.connect("127.0.0.1", 9042);
        Session session = client.getSession();

        final Serde<JsonNode> jsonSerde = getJsonSerde();
        KStreamBuilder builder = new KStreamBuilder();
        KStream<String, JsonNode> coinInfo = builder.stream(Serdes.String(), jsonSerde, "coins-price-info");

        // create the initial json object for balances
        ObjectNode initCoinVolatility= getInitVolatility();

        KTable<String, JsonNode> PriceInfo = coinInfo
                .groupByKey(Serdes.String(), jsonSerde)
                .aggregate(
                        () -> initCoinVolatility,
                        (key, info, volatility) -> generateVolatility(session, key, info, volatility),
                        jsonSerde,
                        "coins-volatility-agg"
                );
        PriceInfo.to(Serdes.String(), jsonSerde, "coins-volatility");

        KafkaStreams streams = new KafkaStreams(builder, settingConfig());
        streams.cleanUp();
        streams.start();

        //print
        System.out.println(streams.toString());
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static ObjectNode getInitVolatility() {
        ObjectNode initCoinVolatility= JsonNodeFactory.instance.objectNode();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        initCoinVolatility.put("id", 0);
        initCoinVolatility.put("created", timeStamp);
        initCoinVolatility.put("volatility", 0);
        return initCoinVolatility;
    }


    private static JsonNode generateVolatility(Session session, String key, JsonNode info, JsonNode volatility) {
        ObjectNode newVolatility = JsonNodeFactory.instance.objectNode();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        double sumOfSquares = info.get("sumOfSquares").asDouble();
        //System.out.println(String.format("sumOfSquares = %d", sumOfSquares));
        double NMeanOfSquares = Math.pow(info.get("sum").asDouble(), 2) / info.get("count").asDouble();
        String vol = String.valueOf(sumOfSquares - NMeanOfSquares);
        newVolatility.put("volatility", sumOfSquares - NMeanOfSquares);
        newVolatility.put("id", key);
        newVolatility.put("created", timeStamp);

        insertOrUpdate(session, key, timeStamp, vol);
        return newVolatility;
    }

    private static void insertOrUpdate(Session session, String key, String timeStamp, String volatility){
        int count = getCount(session, key);
        String query;
        if (count == 0) {
            query = String.format("insert into demo.coins (id, created, volatility) values ('%s', '%s', %s);"
                    ,key, timeStamp, volatility);
        } else {
            query = String.format("update demo.coins set created='%s',volatility=%s where id='%s'"
                    ,timeStamp, volatility, key);
        }
        session.execute(query);
    }

    private static int getCount(Session session, String id) {
        String query = String.format("select count(*) from demo.coins where id = '%s'", id);
        ResultSet result = session.execute(query);
        Row row = result.one();
        return Integer.parseInt(row.getToken("count").toString());
    }

}
