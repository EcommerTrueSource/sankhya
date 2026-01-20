package br.com.sankhya.truebrands.integracaoml;

import br.com.sankhya.jape.core.Jape;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ApiClient {
    private BigDecimal codEmp;
    private DynamicVO configVO;
    private String baseUri = "https://api.mercadolibre.com/";
    private JapeWrapper configDAO = JapeFactory.dao("AD_DADOSML");
    public ApiClient(BigDecimal empresa) throws Exception {
        this.codEmp = empresa;
        configVO = configDAO.findByPK(this.codEmp);
    }
    public void autenticacao(){
        HttpURLConnection connection = null;
        try{
            URL url = new URL(this.baseUri+"oauth/token");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("grant_type","refresh_token");
            body.put("client_id",configVO.asString("CLIENT_ID"));
            body.put("client_secret",configVO.asString("CLIENT_SECRET"));
            body.put("refresh_token",configVO.asString("REFRESH_TOKEN"));

            String jsonString = body.toString();

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int status = connection.getResponseCode();

            InputStream is = (status >= 200 && status < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    String responseBody = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                    if (!responseBody.isEmpty()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (status == HttpURLConnection.HTTP_OK) {
                            IntegracaoHelper.salvaLog(responseBody.toString(),jsonString);
                        } else {
                            System.err.println("Erro da API: " + jsonResponse.optString("message"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            if(connection != null){
                connection.disconnect();
            }
        }
    }

    public void buscaPack(){

    }

    private void atualizaRefreshToken(String refreshToken){
        final BigDecimal finalCodEmp = this.codEmp;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    configDAO.prepareToUpdateByPK(finalCodEmp).set("REFRESH_TOKEN",refreshToken).update();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();

    }
}
