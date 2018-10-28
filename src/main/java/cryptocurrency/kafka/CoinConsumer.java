package cryptocurrency.kafka;

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

public class CoinConsumer {
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
        final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
        final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
        final Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);

        KStreamBuilder builder = new KStreamBuilder();
        KStream<String, String> coins = builder.stream(Serdes.String(), Serdes.String(), "coins2");

        // create the initial json object for balances
        ObjectNode initialPriceInfo = JsonNodeFactory.instance.objectNode();
        initialPriceInfo.put("count", 0);
        initialPriceInfo.put("sum", 0);
        initialPriceInfo.put("sumOfSquares", 0);

        KTable<String, JsonNode> PriceInfo = coins
                .groupByKey(Serdes.String(), Serdes.String())
                .aggregate(
                        () -> initialPriceInfo,
                        (key, price, Info) -> createWholeInfo(price, Info),
                        jsonSerde,
                        "coins-price-agg"
                );
        PriceInfo.to(Serdes.String(), jsonSerde, "coins-price-info");


        KafkaStreams streams = new KafkaStreams(builder, settingConfig());
        streams.cleanUp();
        streams.start();

        //print
        System.out.println(streams.toString());
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
    private static JsonNode createWholeInfo(String price, JsonNode info) {
        ObjectNode newInfo = JsonNodeFactory.instance.objectNode();
        newInfo.put("count", info.get("count").asInt() + 1);
        newInfo.put("sum", info.get("sum").asDouble() + Double.valueOf(price));
        newInfo.put("sumOfSquares", info.get("sumOfSquares").asDouble() + Math.pow(Double.valueOf(price), 2));
        return newInfo;
    }
}
