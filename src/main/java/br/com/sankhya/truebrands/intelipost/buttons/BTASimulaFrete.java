package br.com.sankhya.truebrands.intelipost.buttons;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truebrands.intelipost.CallService;
import br.com.sankhya.truebrands.utils.Utils;
import com.sankhya.util.Base64Impl;
import com.sankhya.util.JdbcUtils;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;

public class BTASimulaFrete implements AcaoRotinaJava {
  private static final String				APP_LINK	= "<a title=\"Abrir Tela\" href=\"/mge/system.jsp#app/{0}/{1}&pk-refresh={3}\" target=\"_top\"><u><b>{2}</b></u></a>";
  public void doAction(ContextoAcao context) throws Exception {

    Registro[] registros = context.getLinhas();
    if (registros.length > 1)
      context.mostraErro("Selecione apenas um registro!"); 
    Registro linha = registros[0];
    if (linha.getCampo("PENDENTE").equals(new String("N")) && linha
      .getCampo("TIPMOV").equals(new String("P")))
      throw new Exception("Pedido faturado não pode ter frete alterado!");
    if (!linha.getCampo("TIPMOV").equals(new String("P")) && 
      Utils.existeOrigem((BigDecimal)linha.getCampo("NUNOTA")))
      throw new Exception("Frete só pode ser alterado no pedido!");
    if (!linha.getCampo("TIPMOV").equals(new String("P")) && 
      !Utils.existeOrigem((BigDecimal)linha.getCampo("NUNOTA")) && linha
      .getCampo("STATUSNOTA").equals(new String("L")))
      throw new Exception("Frete só pode ser alterado em Nota Fiscal não confirmada!");
    String idFrete = (String)linha.getCampo("AD_IDSIMFRETE");
    if (idFrete != null);
    JSONObject body = new JSONObject();
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    PersistentLocalEntity parceiro = dwfEntityFacade.findEntityByPrimaryKey("Parceiro", linha.getCampo("CODPARC"));
    DynamicVO parceiroVO = (DynamicVO)parceiro.getValueObject();
    PersistentLocalEntity empresa = dwfEntityFacade.findEntityByPrimaryKey("Empresa", linha.getCampo("CODEMP"));
    DynamicVO empresaVO = (DynamicVO)empresa.getValueObject();
    body.put("origin_zip_code", empresaVO.getProperty("CEP"));
    body.put("destination_zip_code", parceiroVO.getProperty("CEP"));
    body.put("quoting_mode", "DYNAMIC_BOX_ALL_ITEMS");
    JSONObject identification = new JSONObject();
    identification.put("url", "https://truebrands.sankhyacloud.com.br/");
    body.put("identification", identification);
    JSONObject additional_information = new JSONObject();
    additional_information.put("lead_time_business_days", "");
    additional_information.put("sales_channel", "");
    body.put("additional_information", additional_information);
    JSONArray products = new JSONArray();
    QueryExecutor itens = context.getQuery();
    itens.setParam("NUNOTA", linha.getCampo("NUNOTA"));
    itens.nativeSelect("SELECT PRO.CODPROD, PRO.ALTURA, PRO.LARGURA, PRO.ESPESSURA, ITE.QTDNEG,  PRO.PESObruto, ITE.VLRUNIT, gru.descrgrupoprod FROM TGFITE ITE, TGFPRO PRO, TGFGRU GRU WHERE ITE.NUNOTA={NUNOTA} and ite.codprod=pro.codprod AND PRO.CODGRUPOPROD=GRU.CODGRUPOPROD");
    while (itens.next()) {
      JSONObject item = new JSONObject();
      item.put("height", itens.getDouble("ALTURA"));
      item.put("width", itens.getDouble("LARGURA"));
      item.put("length", itens.getDouble("ESPESSURA"));
      item.put("weight", itens.getDouble("PESObruto"));
      item.put("cost_of_goods", itens.getDouble("VLRUNIT"));
      item.put("quantity", itens.getDouble("QTDNEG"));
      item.put("sku_id", itens.getDouble("CODPROD"));
      item.put("product_category", itens.getString("descrgrupoprod"));
      products.put(item);
    } 
    body.put("products", products);
    try {
      CallService apiService = new CallService();
      apiService.setUri("https://api.intelipost.com.br/api/v1/quote_by_product");
      apiService.setMethod("POST");
      apiService.setBody(body.toString());
      String response = apiService.fire();
      JSONObject json = new JSONObject(response);
      linha.setCampo("AD_IDSIMFRETE", json.getJSONObject("content").get("id").toString());
      DynamicVO simulacaoVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AD_SIMFRETE");
      simulacaoVO.setProperty("ID", json.getJSONObject("content").get("id").toString());
      simulacaoVO.setProperty("NUNOTA", linha.getCampo("NUNOTA"));
      dwfEntityFacade.createEntity("AD_SIMFRETE", (EntityVO)simulacaoVO);
      JSONArray negociacoes = json.getJSONObject("content").getJSONArray("delivery_options");
      for (int i = 0; i < negociacoes.length(); i++) {
        JSONObject negociacao = (JSONObject)negociacoes.get(i);
        BigDecimal freteTotal = new BigDecimal(negociacao.getDouble("final_shipping_cost"), MathContext.DECIMAL64);
        BigDecimal custoReal = new BigDecimal(negociacao.getDouble("provider_shipping_cost"), MathContext.DECIMAL64);
        BigDecimal prazo = new BigDecimal(negociacao.getInt("delivery_estimate_business_days"), MathContext.DECIMAL64);
        DynamicVO negVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AD_SIMFRETENEG");
        negVO.setProperty("ID", json.getJSONObject("content").get("id").toString());
        negVO.setProperty("DESCRICAO", negociacao.getString("description"));
        negVO.setProperty("SEQUENCIA", new BigDecimal(i + 1, MathContext.DECIMAL64));
        negVO.setProperty("FRETETOTAL", freteTotal);
        negVO.setProperty("CUSTOREAL", custoReal);
        negVO.setProperty("PRAZOMINIMO", prazo);
        negVO.setProperty("ATIVA", "N");
        negVO.setProperty("CODTIPFRETE", new BigDecimal(negociacao.getInt("delivery_method_id")));
        negVO.setProperty("CODPARC", buscaCodparc(BigDecimal.valueOf(negociacao.getInt("delivery_method_id"))));
        dwfEntityFacade.createEntity("AD_SIMFRETENEG", (EntityVO)negVO);
      }
      String id = json.getJSONObject("content").get("id").toString();
      String retorno = getLink(id,id);
      context.setMensagemRetorno("Processo Finalizado!<br><br><b>ID Simula" + retorno);
//      context.setMensagemRetorno("Processo Finalizado!<br><br><b>ID Simula" + json.getJSONObject("content").get("id").toString());
    } catch (Exception e) {
      e.printStackTrace();
      context.mostraErro(e.getMessage());
    } 
  }

  private String getLink(String descricao, String id) {
    String pk = "{\"ID\":\"{0}\"}".replace("{0}", id);

    var url = APP_LINK.replace("{0}", Base64Impl.encode("br.com.sankhya.menu.adicional.AD_SIMFRETE".getBytes()).trim());
    url = url.replace("{1}", Base64Impl.encode(pk.getBytes()).trim());
    url = url.replace("{2}", descricao);
    url = url.replace("{3}", "${System.currentTimeMillis()}");

    return url;
  }
  
  private static BigDecimal buscaCodparc(BigDecimal cod) throws Exception {
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = (NativeSql)null;
    ResultSet rs = (ResultSet)null;
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql("select CODPARC from TGFPAR where AD_CODTIPFRETE=" + cod);
      rs = sql.executeQuery();
      if (rs.next())
        return rs.getBigDecimal("CODPARC"); 
    } catch (Exception e) {
      throw new Exception("SQL Error: " + e.getMessage());
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
    return BigDecimal.ZERO;
  }
}
