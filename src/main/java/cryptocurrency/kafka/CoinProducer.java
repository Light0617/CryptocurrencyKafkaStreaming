package cryptocurrency.kafka;

import me.joshmcfarlin.cryptocompareapi.Coins;
import me.joshmcfarlin.cryptocompareapi.CryptoCompareAPI;
import me.joshmcfarlin.cryptocompareapi.Exceptions.OutOfCallsException;
import me.joshmcfarlin.cryptocompareapi.Historic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.*;
import java.util.Properties;
import java.util.stream.Collectors;


public class CoinProducer {
    private CryptoCompareAPI api = new CryptoCompareAPI();

    private static Properties setting(){
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Producer acks
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all"); // strongest producing guarantee
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, "3");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "1");
        // ensure we don't push duplicates
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        return properties;
    }

    private Set<String> getAllCoins() throws IOException, OutOfCallsException {
        Coins coins = new Coins();
        //return coins.getCoinList().getCoins().keySet();
        Set<String> result = new HashSet<>();
        int count = 0;
        for(String coin : coins.getCoinList().getCoins().keySet()){
            result.add(coin);
            count++;
            //if(count >= 4) break;
        }
        return result;
    }

    private List<Double> getPrice(String coin) throws Exception{
        Historic.History history = api.historic.getDay(coin, "USD");
        return history.getData().stream().map(element -> element.getClose()).collect(Collectors.toList());
    }

    public static void main(String[] args) throws Exception {
        CoinProducer coinProducer = new CoinProducer();
        Properties properties = setting();

        Producer<String, String> producer = new KafkaProducer<>(properties);

        System.out.println("Get all coins");
        Set<String> coins = coinProducer.getAllCoins();
        System.out.println("Send all coins and prices");
        for(String coin: coins) {
            System.out.println("coin, " + coin);
            try {
                List<Double> prices = coinProducer.getPrice(coin);
                for(double price: prices){
                    System.out.println(String.format("%s, %s", coin, price));
                    producer.send(new ProducerRecord<>("coins", coin, String.valueOf(price)));
                }
            } catch (InterruptedException e) {
                break;
            }
        }
        producer.close();
    }
}
