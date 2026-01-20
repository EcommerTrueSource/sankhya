import org.json.JSONObject;

public class Testes {
    public static void main(String[] args) {
        JSONObject body = new JSONObject();
        body.put("TESTE","ASDFS");
        body.put("SDIFHASDIF","11111");

        JSONObject novo = new JSONObject();
        novo.put("NOVO","KKKKK");
        body.put("novo",novo);
        System.out.println(body);
    }
}
