package br.com.sankhya.truebrands.intelipost.buttons;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import br.com.sankhya.truebrands.intelipost.IntelipostService;
import org.json.JSONArray;
import org.json.JSONObject;

public class BTAAtualizarStatusDespacho implements AcaoRotinaJava {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  
  public void doAction(ContextoAcao context) throws Exception {
    Registro[] registros = context.getLinhas();
    for (Registro linha : registros) {
      DynamicVO tgfCabVO = JapeFactory.dao("CabecalhoNota").findByPK(new Object[] { linha.getCampo("NUNOTA") });
      if (!"C".equals(tgfCabVO.asString("AD_STATUSINTELIPOST")))
        context.mostraErro("Nota/pedido nfoi enviado para a Intelipost!"); 
      DynamicVO ordemCargaVO = JapeFactory.dao("OrdemCarga").findByPK(new Object[] { tgfCabVO.asBigDecimal("CODEMP"), tgfCabVO.asBigDecimal("ORDEMCARGA") });
      if (!"F".equals(ordemCargaVO.asString("SITUACAO")))
        context.mostraErro("Ordem de Carga precisa estar com situafechada!"); 
      BigDecimal order_number = tgfCabVO.asBigDecimal("NUNOTA");
      String event_date = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")).format(new Date());
      IntelipostService intelipostService = new IntelipostService();
      try {
        intelipostService.marcarPedidosComoExpedidosComData((new JSONArray()).put((new JSONObject()).put("order_number", order_number).put("event_date", event_date)));
      } catch (Exception e) {
        context.mostraErro("Erro ao atualizar status do despacho: " + e.getMessage());
      } 
      linha.setCampo("AD_STATUSPED", "D");
    } 
    context.setMensagemRetorno("Status do despacho atualizado com sucesso!");
  }
}
