package br.com.sankhya.truebrands.intelipost;

import br.com.sankhya.jape.vo.DynamicVO;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import br.com.sankhya.truebrands.utils.ConfiguracaoDAO;
import org.apache.commons.io.input.CloseShieldInputStream;

public class CallService {
  private String uri;

  private String method;

  private String body;

  private boolean gzipEnabled = false;

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

  public void enableGzipCompression(boolean enabled) {
    this.gzipEnabled = enabled;
  }

  public String fire() throws Exception {
    HttpURLConnection connection = null;
    BufferedReader reader = null;
    try {
      DynamicVO configVO = ConfiguracaoDAO.get();
      String token = configVO.asString("APIKEY");
      StringBuffer responseContent = new StringBuffer();
      URL url = new URL(this.uri);
      connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod(this.method);
      connection.setRequestProperty("api-key", token);
      connection.setRequestProperty("content-type", "application/json;charset=utf-8");
      connection.setRequestProperty("Accept", "application/json");
      connection.setConnectTimeout(30000);
      connection.setReadTimeout(30000);
      connection.setDoOutput(true);
      connection.setDoInput(true);
      if (this.gzipEnabled)
        connection.setRequestProperty("Content-Encoding", "gzip");
      if (this.body != null) {
        OutputStream os = connection.getOutputStream();
        if (this.gzipEnabled) {
          try (GZIPOutputStream gos = new GZIPOutputStream(os)) {
            gos.write(this.body.getBytes("UTF-8"));
            gos.finish();
          }
        } else {
          try (OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8")) {
            osw.write(this.body);
            osw.flush();
          }
        }
      }
      int status = connection.getResponseCode();
      if (status > 299) {
        CloseShieldInputStream closeShieldInputStream1 = new CloseShieldInputStream(connection.getErrorStream());
        reader = new BufferedReader(new InputStreamReader((InputStream)closeShieldInputStream1));
        String str;
        while ((str = reader.readLine()) != null)
          responseContent.append(str);
        throw new Exception(responseContent.toString());
      }
      CloseShieldInputStream closeShieldInputStream = new CloseShieldInputStream(connection.getInputStream());
      reader = new BufferedReader(new InputStreamReader((InputStream)closeShieldInputStream, "UTF-8"));
      String line;
      while ((line = reader.readLine()) != null)
        responseContent.append(line);
      return responseContent.toString();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null)
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      if (connection != null)
        connection.disconnect();
    }
    return null;
  }
}
