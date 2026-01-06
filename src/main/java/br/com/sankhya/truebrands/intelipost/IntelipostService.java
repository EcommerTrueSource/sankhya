package br.com.sankhya.truebrands.intelipost;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.truebrands.utils.ConfiguracaoDAO;
import org.json.JSONArray;
import org.json.JSONObject;

public class IntelipostService {
  public JSONObject consultarPedido(String numeroPedido) throws Exception {
    DynamicVO configVO = ConfiguracaoDAO.get();
    String baseUrl = configVO.asString("API");
    if (baseUrl == null || baseUrl.trim().isEmpty())
      throw new Exception("URL base da Intelipost nestconfigurada na tabela AD_CFGINT."); 
    String endpoint = baseUrl + "/shipment_order/" + numeroPedido;
    CallService callService = new CallService();
    callService.setUri(endpoint);
    callService.setMethod("GET");
    String response = callService.fire();
    if (response != null && !response.isEmpty())
      return new JSONObject(response); 
    return null;
  }
  
  public JSONObject getEtiqueta(String numeroPedido, String quantidadeVolumes) throws Exception {
    DynamicVO configVO = ConfiguracaoDAO.get();
    String baseUrl = configVO.asString("API");
    if (baseUrl == null || baseUrl.trim().isEmpty())
      throw new Exception("URL base da Intelipost nestconfigurada na tabela AD_CFGINT."); 
    String endpoint = baseUrl + "/shipment_order/get_label/" + numeroPedido + "/" + quantidadeVolumes;
    CallService callService = new CallService();
    callService.setUri(endpoint);
    callService.setMethod("GET");
    String response = callService.fire();
    if (response != null && !response.isEmpty())
      return new JSONObject(response); 
    return null;
  }
  
  public JSONObject marcarPedidosComoExpedidosComData(JSONArray pedidos) throws Exception {
    DynamicVO configVO = ConfiguracaoDAO.get();
    String baseUrl = configVO.asString("API");
    if (baseUrl == null || baseUrl.trim().isEmpty())
      throw new Exception("URL base da Intelipost nestconfigurada na tabela AD_CFGINT."); 
    String endpoint = baseUrl + "/shipment_order/multi/shipped/with_date";
    CallService callService = new CallService();
    callService.setUri(endpoint);
    callService.setMethod("POST");
    callService.setBody((pedidos != null) ? pedidos.toString() : "[]");
    String response = callService.fire();
    if (response != null && !response.isEmpty()) {
      String trimmed = response.trim();
      if (trimmed.startsWith("{"))
        return new JSONObject(trimmed); 
      JSONObject wrapper = new JSONObject();
      wrapper.put("content", new JSONArray(trimmed));
      return wrapper;
    } 
    return null;
  }
  
  public JSONObject marcarPedidosProntosParaEnvioComData(JSONArray pedidos) throws Exception {
    DynamicVO configVO = ConfiguracaoDAO.get();
    String baseUrl = configVO.asString("API");
    if (baseUrl == null || baseUrl.trim().isEmpty())
      throw new Exception("URL base da Intelipost nestconfigurada na tabela AD_CFGINT."); 
    String endpoint = baseUrl + "/shipment_order/multi/ready_for_shipment/with_date";
    CallService callService = new CallService();
    callService.setUri(endpoint);
    callService.setMethod("POST");
    callService.setBody((pedidos != null) ? pedidos.toString() : "[]");
    String response = callService.fire();
    if (response != null && !response.isEmpty()) {
      String trimmed = response.trim();
      if (trimmed.startsWith("{"))
        return new JSONObject(trimmed); 
      JSONObject wrapper = new JSONObject();
      wrapper.put("content", new JSONArray(trimmed));
      return wrapper;
    } 
    return null;
  }
}
