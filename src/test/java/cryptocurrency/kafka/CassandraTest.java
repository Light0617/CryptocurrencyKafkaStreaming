package cryptocurrency.kafka;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.junit.*;
import java.util.*;


public class CassandraTest {
    private Session session;
    private CassandraConnector client;
    @Before
    public void connect() {
        client = new CassandraConnector();
        client.connect("127.0.0.1", 9042);
        session = client.getSession();
    }
    @Test
    public void selectAll() {
        StringBuilder sb =
                new StringBuilder("SELECT * FROM demo.coins");

        String query = sb.toString();
        ResultSet result = session.execute(query);

        List<Coin> coins = new ArrayList<>();
        for (Row row : result.all()) {
            coins.add(new Coin(row.getString("id"), row.getString("created"), row.getFloat("volatility")));
        }
        for(Coin c : coins) {
            System.out.println(c);
        }
    }
    @Test
    public void canInsert() {
        String id = "124";
        String created = "11/01";
        String volatility ="11.12";

        StringBuilder sb =
                new StringBuilder(String.format("insert into demo.coins (id, created, volatility) values ('%s', '%s', %s);"
                ,id, created, volatility));
        String query = sb.toString();
        session.execute(query);
    }

    @Test
    public void selectCount() {
        String id = "300";
        String query = String.format("select count(*) from demo.coins where id = '%s'", id);
        ResultSet result = session.execute(query);
        Row row = result.one();
        int count = Integer.parseInt(row.getToken("count").toString());
        System.out.println(count);


        query = String.format("select count(*) from demo.coins where id = '39999'");
        result = session.execute(query);
        row = result.one();
        System.out.println(row.getToken("count"));
    }

    @Test void createTable() {
        String tableName = "coins2";
        String query = String.format("create table %s (id text, created text, volatility float, primary key(id));", tableName);
        session.execute(query);
    }
}
