package br.com.sankhya.truebrands.intelipost.events;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

import br.com.sankhya.truebrands.intelipost.CallService;
import org.json.JSONArray;
import org.json.JSONObject;

public class EvtEnviarDocumento implements EventoProgramavelJava {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  
  public void afterInsert(PersistenceEvent event) throws Exception {}
  
  public void afterUpdate(PersistenceEvent event) throws Exception {
    JapeSession.SessionHandle hnd = null;
    DynamicVO tgfCabVO = null;
    try {
      hnd = JapeSession.open();
      tgfCabVO = (DynamicVO)event.getVo();
      JdbcWrapper jdbc = null;
      EntityFacade dwf = EntityFacadeFactory.getDWFFacade();
      jdbc = dwf.getJdbcWrapper();
      StringBuilder mensagem = new StringBuilder();
      mensagem.append("<strong>Processo Finalizado!</strong><br><br>");
      if (!"A".equals(tgfCabVO.asString("STATUSNFE")))
        return;
      if ("C".equals(tgfCabVO.asString("AD_STATUSINTELIPOST")))
        return;
      Collection<DynamicVO> resultadosTOP = JapeFactory.dao("TipoOperacao").find("CODTIPOPER = ?", new Object[] { tgfCabVO.asBigDecimal("CODTIPOPER") });
      DynamicVO tgfTopVO = resultadosTOP.stream().sorted(Comparator.comparing(vo -> vo.asBigDecimal("DHALTER"))).findFirst().orElse(null);
      String topLiberada = tgfTopVO.asString("AD_INTINTELIPOST");
      if (!"S".equals(topLiberada))
        return; 
      JSONObject body = new JSONObject();
      PersistentLocalEntity parceiro = dwf.findEntityByPrimaryKey("Parceiro", tgfCabVO.asBigDecimalOrZero("CODPARC"));
      DynamicVO parceiroVO = (DynamicVO)parceiro.getValueObject();
      PersistentLocalEntity transportadora = dwf.findEntityByPrimaryKey("Parceiro", tgfCabVO.asBigDecimalOrZero("CODPARCTRANSP"));
      DynamicVO parceiroTransportadoraVO = (DynamicVO)transportadora.getValueObject();
      DynamicVO contatoVO = null;
      BigDecimal codContato = tgfCabVO.asBigDecimalOrZero("CODCONTATO");
      if (codContato.compareTo(new BigDecimal(0)) != 0)
        contatoVO = JapeFactory.dao("Contato").findByPK(new Object[] { tgfCabVO.asBigDecimalOrZero("CODPARC"), codContato }); 
      if (tgfCabVO.asBigDecimalOrZero("CODPARCTRANSP").compareTo(new BigDecimal(0)) == 0)
        return; 
      PersistentLocalEntity empresa = dwf.findEntityByPrimaryKey("Empresa", tgfCabVO.asBigDecimal("CODEMP"));
      DynamicVO empresaVO = (DynamicVO)empresa.getValueObject();
      PersistentLocalEntity cidade = dwf.findEntityByPrimaryKey("Cidade", (contatoVO == null) ? parceiroVO.asBigDecimalOrZero("CODCID") : contatoVO.asBigDecimalOrZero("CODCID"));
      DynamicVO cidadeVO = (DynamicVO)cidade.getValueObject();
      PersistentLocalEntity bairro = dwf.findEntityByPrimaryKey("Bairro", (contatoVO == null) ? parceiroVO.asBigDecimalOrZero("CODBAI") : contatoVO.asBigDecimalOrZero("CODBAI"));
      DynamicVO bairroVO = (DynamicVO)bairro.getValueObject();
      PersistentLocalEntity enderecoEntity = dwf.findEntityByPrimaryKey("Endereco", (contatoVO == null) ? parceiroVO.asBigDecimalOrZero("CODEND") : contatoVO.asBigDecimalOrZero("CODEND"));
      DynamicVO enderecoVO = (DynamicVO)enderecoEntity.getValueObject();
      PersistentLocalEntity estado = dwf.findEntityByPrimaryKey("UnidadeFederativa", cidadeVO.getProperty("UF"));
      DynamicVO estadoVO = (DynamicVO)estado.getValueObject();
      Timestamp date = new Timestamp(System.currentTimeMillis());
      Date dateIni = new Date(date.getTime());
      Timestamp previsaoentrega = new Timestamp(System.currentTimeMillis());
      if (!parceiroVO.asString("NOMEPARC").contains(new String(" ")))
        throw new Exception("Parceiro " + parceiroVO.asBigDecimal("CODPARC") + " - " + parceiroVO.asString("NOMEPARC") + " sem sobrenome"); 
      SimpleDateFormat formatoData = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      if (parceiroTransportadoraVO.asBigDecimalOrZero("AD_CODTIPFRETE").compareTo(new BigDecimal(0)) == 0)
        return; 
      if (tgfCabVO.asTimestamp("DTPREVENT") != null)
        previsaoentrega = tgfCabVO.asTimestamp("DTPREVENT"); 
      BigDecimal codTOP = tgfCabVO.asBigDecimalOrZero("CODTIPOPER");
      String nomesales = "";
      if (codTOP.compareTo(new BigDecimal(1101)) == 0) {
        nomesales = "E-commerce";
      } else if (codTOP.compareTo(new BigDecimal(1102)) == 0) {
        nomesales = "B2B";
      } else if (codTOP.compareTo(new BigDecimal(1110)) == 0) {
        nomesales = "Marketing";
      } else {
        nomesales = "sankhya";
      } 
      body.put("order_number", tgfCabVO.asBigDecimal("NUNOTA"));
      body.put("origin_federal_tax_payer_id", empresaVO.asString("CGC"));
      body.put("origin_warehouse_code", empresaVO.asBigDecimal("CODEMP"));
      body.put("customer_shipping_costs", tgfCabVO.asBigDecimalOrZero("VLRDESTAQUE"));
      body.put("sales_channel", nomesales);
      body.put("scheduled", false);
      body.put("created", tgfCabVO.asTimestamp("DTNEG"));
      body.put("origin_zip_code", empresaVO.asString("CEP"));
      body.put("estimated_delivery_date", formatoData.format(previsaoentrega));
      body.put("delivery_method_id", parceiroTransportadoraVO.asBigDecimalOrZero("AD_CODTIPFRETE"));
      body.put("delivery_method_external_id", parceiroTransportadoraVO.asBigDecimalOrZero("AD_CODTIPFRETE"));
      JSONObject end_customer = new JSONObject();
      String Telefone = (contatoVO == null) ? parceiroVO.asString("TELEFONE") : ((contatoVO.asString("TELEFONE") == null) ? "" : contatoVO.asString("TELEFONE"));
      BigDecimal QtdCaracteres = new BigDecimal(parceiroVO.asString("NOMEPARC").length());
      end_customer.put("first_name", parceiroVO.asString("NOMEPARC").substring(0, parceiroVO.asString("NOMEPARC").indexOf(" ")));
      end_customer.put("last_name", parceiroVO.asString("NOMEPARC").substring(parceiroVO.asString("NOMEPARC").substring(0, parceiroVO.asString("NOMEPARC").indexOf(" ")).length()));
      end_customer.put("email", (contatoVO == null) ? parceiroVO.asString("EMAIL") : ((contatoVO.asString("EMAIL") == null) ? "" : contatoVO.asString("EMAIL")));
      end_customer.put("phone", formataDados(Telefone));
      end_customer.put("cellphone", formataDados(Telefone));
      end_customer.put("is_company", parceiroVO.asString("TIPPESSOA").equals("J"));
      end_customer.put("federal_tax_payer_id", parceiroVO.asString("CGC_CPF"));
      end_customer.put("state_tax_payer_id", parceiroVO.asString("IDENTINSCESTAD"));
      end_customer.put("shipping_country", "Brasil");
      end_customer.put("shipping_state", estadoVO.getProperty("UF"));
      end_customer.put("shipping_city", cidadeVO.getProperty("NOMECID"));
      end_customer.put("shipping_additional", (contatoVO == null) ? parceiroVO.asString("COMPLEMENTO") : ((contatoVO.asString("COMPLEMENTO") == null) ? "" : contatoVO.asString("COMPLEMENTO")));
      end_customer.put("shipping_address", enderecoVO.getProperty("NOMEEND"));
      end_customer.put("shipping_number", (contatoVO == null) ? ((parceiroVO.asString("NUMEND") == null) ? "SN" : parceiroVO.asString("NUMEND")) : ((contatoVO.asString("NUMEND") == null) ? "SN" : contatoVO.asString("NUMEND")));
      end_customer.put("shipping_quarter", bairroVO.getProperty("NOMEBAI"));
      end_customer.put("shipping_zip_code", (contatoVO == null) ? parceiroVO.asString("CEP") : contatoVO.asString("CEP"));
      body.put("end_customer", end_customer);
      JSONArray shipment_order_volume_array = new JSONArray();
      NativeSql sql = (NativeSql)null;
      ResultSet volumesPedido = (ResultSet)null;
      sql = new NativeSql(jdbc);
      sql.appendSql("select\n" +
              "level,\n" +
              "a.*\n" +
              "from(\n" +
              "select\n" +
              "nvl(cab.qtdvol,0) as qtdvol,\n" +
              "sum(ite.vlrtot-ite.vlrdesc)/nvl(cab.qtdvol,0) as vlrtotitens,\n" +
              "sum(ite.qtdneg*pro.pesobruto)/nvl(cab.qtdvol,0) as weight,\n" +
              "sum(ite.qtdneg*pro.largura)/nvl(cab.qtdvol,0) as width,\n" +
              "sum(ite.qtdneg*pro.altura)/nvl(cab.qtdvol,0) as height,\n" +
              "sum(ite.qtdneg*pro.espessura)/nvl(cab.qtdvol,0) as length,\n" +
              "sum(ite.qtdneg)/nvl(cab.qtdvol,0) as products_quantity\n" +
              "from tgfite ite\n" +
              "inner join tgfcab cab on (ite.nunota = cab.nunota)\n" +
              "inner join tgfpro pro on pro.codprod=ite.codprod\n" +
              "inner join tgfvol vol on vol.codvol=ite.codvol\n" +
              "where cab.NUNOTA= " + tgfCabVO.asBigDecimalOrZero("NUNOTA") +
              "\ngroup by\ncab.qtdvol\n)a\nconnect by level  <= A.QTDVOL");
      volumesPedido = sql.executeQuery();
      int count = 1;
      while (volumesPedido.next()) {
        JSONObject volume = new JSONObject();
        volume.put("name", "CAIXA");
        volume.put("shipment_order_volume_number", count);
        volume.put("volume_type_code", "BOX");
        volume.put("weight", volumesPedido.getDouble("weight"));
        volume.put("width", volumesPedido.getDouble("width"));
        volume.put("height", volumesPedido.getDouble("height"));
        volume.put("length", volumesPedido.getDouble("length"));
        volume.put("products_quantity", volumesPedido.getDouble("products_quantity"));
        volume.put("products_nature", "PRODUTOS");
        JSONObject shipment_order_volume_invoice = new JSONObject();
        shipment_order_volume_invoice.put("invoice_series", tgfCabVO.asString("SERIENOTA"));
        shipment_order_volume_invoice.put("invoice_number", tgfCabVO.asBigDecimalOrZero("NUMNOTA"));
        shipment_order_volume_invoice.put("invoice_key", tgfCabVO.asString("CHAVENFE"));
        shipment_order_volume_invoice.put("invoice_date", formatoData.format(tgfCabVO.asTimestamp("DTFATUR")));
        shipment_order_volume_invoice.put("invoice_total_value", tgfCabVO.asBigDecimalOrZero("VLRNOTA"));
        shipment_order_volume_invoice.put("invoice_products_value", volumesPedido.getDouble("vlrtotitens"));
        PersistentLocalEntity cidadeEmpresa = dwf.findEntityByPrimaryKey("Cidade", empresaVO.asBigDecimalOrZero("CODCID"));
        DynamicVO cidadeEmpresaVO = (DynamicVO)cidadeEmpresa.getValueObject();
        PersistentLocalEntity estadoEmpresa = dwf.findEntityByPrimaryKey("UnidadeFederativa", cidadeEmpresaVO.getProperty("UF"));
        DynamicVO estadoEmpresaVO = (DynamicVO)estadoEmpresa.getValueObject();
        BigDecimal cfop = BigDecimal.ZERO;
        if (estadoEmpresaVO.asString("UF").equals(estadoVO.asString("UF"))) {
          cfop = tgfTopVO.asBigDecimalOrZero("CODCFO_SAIDA");
        } else {
          cfop = tgfTopVO.asBigDecimalOrZero("CODCFO_SAIDA_FORA");
        } 
        shipment_order_volume_invoice.put("invoice_cfop", cfop);
        volume.put("shipment_order_volume_invoice", shipment_order_volume_invoice);
        shipment_order_volume_array.put(volume);
        count++;
      }
      body.put("shipment_order_volume_array", shipment_order_volume_array);
      try {
        CallService apiService = new CallService();
        apiService.setUri("https://api.intelipost.com.br/api/v1/shipment_order");
        apiService.setMethod("POST");
        apiService.setBody(body.toString());
        String response = apiService.fire();
        JSONObject json = new JSONObject(response);
        JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
        ((FluidUpdateVO)((FluidUpdateVO)((FluidUpdateVO)cabDAO.prepareToUpdate(tgfCabVO)
          .set("AD_IDINTELIPOST", json.getJSONObject("content").get("id").toString()))
          .set("DTPREVENT", previsaoentrega))
          .set("AD_STATUSINTELIPOST", "C"))
          .update();
      } catch (Exception exception) {
        exception.printStackTrace();
        System.out.println("Erro ao enviar para Intelipost: " + exception.getMessage());
        JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
        ((FluidUpdateVO)cabDAO.prepareToUpdate(tgfCabVO)
          .set("AD_STATUSINTELIPOST", "E"))
          .update();
      } 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    } finally {
      JapeSession.close(hnd);
    } 
  }
  
  public void beforeUpdate(PersistenceEvent event) throws Exception {}
  
  public void afterDelete(PersistenceEvent event) throws Exception {}
  
  public void beforeCommit(TransactionContext event) throws Exception {}
  
  public void beforeDelete(PersistenceEvent event) throws Exception {}
  
  public void beforeInsert(PersistenceEvent event) throws Exception {}
  
  public static String formataDados(String dado) {
    String dados = dado.replaceAll("[^0-9]+", "");
    return dados;
  }
  
  public static Integer ContarDados(String dado) {
    int dados = dado.length();
    return Integer.valueOf(dados);
  }
}
