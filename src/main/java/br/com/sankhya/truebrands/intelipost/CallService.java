package br.com.sankhya.truebrands.intelipost;

import br.com.sankhya.jape.vo.DynamicVO;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import br.com.sankhya.truebrands.utils.ConfiguracaoDAO;
import org.apache.commons.io.input.CloseShieldInputStream;

public class CallService {
  private String uri;
  
  private String method;
  
  private String body;
  
  private static HttpURLConnection connection;
  
  public String getUri() {
    return this.uri;
  }
  
  public void setUri(String uri) {
    this.uri = uri;
  }
  
  public String getMethod() {
    return this.method;
  }
  
  public void setMethod(String method) {
    this.method = method;
  }
  
  public String getBody() {
    return this.body;
  }
  
  public void setBody(String body) {
    this.body = body;
  }
  
  public String fire() throws Exception {
    DynamicVO configVO = ConfiguracaoDAO.get();
    String token = configVO.asString("AMBIENTE").equals(new String("WWW")) ? configVO.asString("TOKENPROD") : configVO.asString("TOKENTEST");
    StringBuffer responseContent = new StringBuffer();
    URL url = new URL(this.uri);
    connection = (HttpURLConnection)url.openConnection();
    connection.setRequestMethod(this.method);
    connection.setRequestProperty("api-key", token);
    connection.setRequestProperty("content-type", "application/json;charset=utf-8");
    connection.setConnectTimeout(30000);
    connection.setReadTimeout(30000);
    connection.setDoOutput(true);
    connection.setDoInput(true);
    if (this.body != null) {
      OutputStream os = connection.getOutputStream();
      OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
      osw.write(this.body);
      osw.flush();
      osw.close();
    } 
    int status = connection.getResponseCode();
    System.out.println("Response VTEX:");
    System.out.println(connection.getResponseMessage());
    System.out.println(status);
    if (status > 299) {
      CloseShieldInputStream closeShieldInputStream1 = new CloseShieldInputStream(connection.getErrorStream());
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)closeShieldInputStream1));
      String str;
      while ((str = bufferedReader.readLine()) != null)
        responseContent.append(str); 
      bufferedReader.close();
      throw new Exception(responseContent.toString());
    } 
    CloseShieldInputStream closeShieldInputStream = new CloseShieldInputStream(connection.getInputStream());
    BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream)closeShieldInputStream, "UTF-8"));
    String line;
    while ((line = reader.readLine()) != null)
      responseContent.append(line); 
    reader.close();
    return responseContent.toString();
  }
}
