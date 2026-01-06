package br.com.sankhya.truebrands.intelipost.buttons;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;

import br.com.sankhya.truebrands.intelipost.CallService;
import org.json.JSONArray;
import org.json.JSONObject;

public class BTABuscarIDIntelipost implements AcaoRotinaJava {
  public void doAction(ContextoAcao context) throws Exception {
    Registro[] registros = context.getLinhas();
    if (registros.length > 1)
      context.mostraErro("Selecione apenas um registro!"); 
    Registro linha = registros[0];
    StringBuilder mensagem = new StringBuilder();
    mensagem.append("<strong>Processo Finalizado!</strong><br><br>");
    String queryPk = "ORDEMCARGA=" + linha.getCampo("ORDEMCARGA").toString() + " AND TIPMOV='V' AND NVL(AD_IDINTELIPOST,0) = 0 AND CODEMP=" + linha.getCampo("CODEMP").toString();
    FinderWrapper f = new FinderWrapper("CabecalhoNota", queryPk);
    EntityFacade dwf = EntityFacadeFactory.getDWFFacade();
    Collection<PersistentLocalEntity> rPLES = dwf.findByDynamicFinder(f);
    for (PersistentLocalEntity rPLE : rPLES) {
      EntityVO rEVO = rPLE.getValueObject();
      DynamicVO tgfCabVO = (DynamicVO)rEVO;
      try {
        CallService apiService = new CallService();
        apiService.setUri("https://api.intelipost.com.br/api/v1/shipment_order/" + tgfCabVO.asString("AD_PEDIDO"));
        apiService.setMethod("GET");
        String response = apiService.fire();
        JSONObject json = new JSONObject(response);
        if (json.get("status").equals(new String("OK"))) {
          JSONObject content = json.getJSONObject("content");
          JSONArray order_array = content.getJSONArray("shipment_order_volume_array");
          for (int p = 0; p < order_array.length(); p++) {
            JSONObject order = (JSONObject)order_array.get(p);
            JSONObject shipment_order_volume_invoice = order.getJSONObject("shipment_order_volume_invoice");
            BigDecimal invoice_number = new BigDecimal(shipment_order_volume_invoice.getDouble("invoice_number"), MathContext.DECIMAL64);
            if (!invoice_number.equals(tgfCabVO.asBigDecimal("NUMNOTA"))) {
              CallService apiService2 = new CallService();
              apiService2.setUri("https://api.intelipost.com.br/api/v1/shipment_order/" + tgfCabVO.asString("AD_PEDIDO") + "_" + tgfCabVO.asBigDecimal("NUMNOTA"));
              apiService2.setMethod("GET");
              String response2 = apiService2.fire();
              JSONObject json2 = new JSONObject(response2);
              if (json2.get("status").equals(new String("OK"))) {
                JSONObject retorno = json2.getJSONObject("content");
                tgfCabVO.setProperty("AD_IDINTELIPOST", retorno.get("id").toString());
                rPLE.setValueObject(rEVO);
              } 
            } else {
              tgfCabVO.setProperty("AD_IDINTELIPOST", content.get("id").toString());
              rPLE.setValueObject(rEVO);
            } 
          } 
        } 
      } catch (Exception exception) {}
    } 
    context.setMensagemRetorno(mensagem.toString());
  }
}
