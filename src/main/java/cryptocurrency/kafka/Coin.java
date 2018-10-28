package cryptocurrency.kafka;

public class Coin {
    private String id;
    private String created;
    private float volatility;
    public Coin(String id, String created, float volatility) {
        this.id = id;
        this.created = created;
        this.volatility = volatility;
    }

    public String toString() {
        return "id= " + id
                + ", created= " + created
                + ", volatility=" + volatility;
    }
}
