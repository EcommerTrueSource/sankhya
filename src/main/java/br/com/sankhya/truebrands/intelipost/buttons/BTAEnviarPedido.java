package br.com.sankhya.truebrands.intelipost.buttons;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truebrands.intelipost.CallService;
import com.sankhya.util.JdbcUtils;
import com.sankhya.util.TimeUtils;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

public class BTAEnviarPedido implements AcaoRotinaJava {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  
  public void doAction(ContextoAcao context) throws Exception {
    Registro[] registros = context.getLinhas();
    JdbcWrapper jdbc = null;
    EntityFacade dwf = EntityFacadeFactory.getDWFFacade();
    jdbc = dwf.getJdbcWrapper();
    if (registros.length > 1)
      context.mostraErro("Selecione apenas um registro!"); 
    Registro linha = registros[0];
    BigDecimal tipoFrete = (linha.getCampo("CODTIPFRETE") == null) ? new BigDecimal(0) : (BigDecimal)linha.getCampo("CODTIPFRETE");
    if (tipoFrete.equals(new BigDecimal(195)))
      throw new Exception("Ordem de carga com modalidade de frete retirar na loja npode ser enviada para a Intelipost!"); 
    if (linha.getCampo("STATUS").equals(new String("F")))
      throw new Exception("Ordem de Carga fechada npode ser enviada para Intelipost!"); 
    StringBuilder mensagem = new StringBuilder();
    mensagem.append("<strong>Processo Finalizado!</strong><br><br>");
    JSONObject body = new JSONObject();
    int naoEnvia = 0;
    String queryPk1 = "ID=" + linha.getCampo("ID");
    FinderWrapper f1 = new FinderWrapper("AD_TGFORDI", queryPk1);
    Collection<PersistentLocalEntity> rPLES1 = dwf.findByDynamicFinder(f1);
    for (PersistentLocalEntity rPLE1 : rPLES1) {
      EntityVO rEVO1 = rPLE1.getValueObject();
      DynamicVO rVO1 = (DynamicVO)rEVO1;
      if (semOrdemCarga(rVO1.asBigDecimal("NUNOTA"))) {
        mensagem.append("Nro. " + rVO1.asBigDecimal("NUNOTA").toString() + " sem Ordem de Carga\n");
        naoEnvia++;
      } 
      BigDecimal notaFiscal = (rVO1.asBigDecimal("NUNOTA") == null) ? new BigDecimal(0) : rVO1.asBigDecimal("NUNOTA");
      if (notaFiscal.equals(new BigDecimal(0)))
        throw new Exception("Nota Fisca nembarcada! Chave NFe: " + rVO1.asString("CHAVENFE")); 
    } 
    if (naoEnvia == 0) {
      String queryPk = "ORDEMCARGA=" + linha.getCampo("ORDEMCARGA").toString() + " AND TIPMOV IN ('V','T') AND NVL(AD_IDINTELIPOST,0) = 0 AND CODEMP=" + linha.getCampo("CODEMP").toString();
      FinderWrapper f = new FinderWrapper("CabecalhoNota", queryPk);
      Collection<PersistentLocalEntity> rPLES = dwf.findByDynamicFinder(f);
      for (PersistentLocalEntity rPLE : rPLES) {
        EntityVO rEVO = rPLE.getValueObject();
        DynamicVO tgfCabVO = (DynamicVO)rEVO;
        String pedido_intelipost = tgfCabVO.asString("AD_PEDIDO");
        try {
          CallService apiService2 = new CallService();
          apiService2.setUri("https://api.intelipost.com.br/api/v1/shipment_order/" + tgfCabVO.asString("AD_PEDIDO"));
          apiService2.setMethod("GET");
          String response2 = apiService2.fire();
          JSONObject json2 = new JSONObject(response2);
          if (json2.get("status").equals(new String("OK"))) {
            JSONObject content = json2.getJSONObject("content");
            JSONArray order_array = content.getJSONArray("shipment_order_volume_array");
            for (int p = 0; p < order_array.length(); p++) {
              JSONObject order = (JSONObject)order_array.get(p);
              JSONObject shipment_order_volume_invoice = order.getJSONObject("shipment_order_volume_invoice");
              BigDecimal invoice_number = new BigDecimal(shipment_order_volume_invoice.getDouble("invoice_number"), MathContext.DECIMAL64);
              if (!invoice_number.equals(tgfCabVO.asBigDecimal("NUMNOTA"))) {
                pedido_intelipost = tgfCabVO.asString("AD_PEDIDO") + "_" + tgfCabVO.asBigDecimal("NUMNOTA");
              } else {
                context.mostraErro("NF com n<b>" + tgfCabVO.asBigDecimalOrZero("NUNOTA").toString() + "</b> jesta integrada com a Intelipost!!");
              } 
            } 
          } 
        } catch (Exception exception) {}
        PersistentLocalEntity parceiro = dwf.findEntityByPrimaryKey("Parceiro", tgfCabVO.asBigDecimalOrZero("CODPARC"));
        DynamicVO parceiroVO = (DynamicVO)parceiro.getValueObject();
        PersistentLocalEntity empresa = dwf.findEntityByPrimaryKey("Empresa", linha.getCampo("CODEMP"));
        DynamicVO empresaVO = (DynamicVO)empresa.getValueObject();
        PersistentLocalEntity cidade = dwf.findEntityByPrimaryKey("Cidade", parceiroVO.asBigDecimalOrZero("CODCID"));
        DynamicVO cidadeVO = (DynamicVO)cidade.getValueObject();
        PersistentLocalEntity bairro = dwf.findEntityByPrimaryKey("Bairro", parceiroVO.asBigDecimalOrZero("CODBAI"));
        DynamicVO bairroVO = (DynamicVO)bairro.getValueObject();
        PersistentLocalEntity enderecoEntity = dwf.findEntityByPrimaryKey("Endereco", parceiroVO.asBigDecimalOrZero("CODEND"));
        DynamicVO enderecoVO = (DynamicVO)enderecoEntity.getValueObject();
        PersistentLocalEntity estado = dwf.findEntityByPrimaryKey("UnidadeFederativa", cidadeVO.getProperty("UF"));
        DynamicVO estadoVO = (DynamicVO)estado.getValueObject();
        Timestamp date = new Timestamp(System.currentTimeMillis());
        Date dateIni = new Date(date.getTime());
        Timestamp previsaoentrega = new Timestamp(System.currentTimeMillis());
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
        SimpleDateFormat formatoData = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        body.put("order_number", pedido_intelipost);
        body.put("customer_shipping_costs", tgfCabVO.asBigDecimalOrZero("VLRDESTAQUE"));
        body.put("sales_channel", nomesales);
        body.put("scheduled", false);
        body.put("created", tgfCabVO.asTimestamp("DTNEG"));
        body.put("delivery_method_id", tgfCabVO.asBigDecimalOrZero("AD_CODTIPFRETE"));
        body.put("delivery_method_external_id", tgfCabVO.asBigDecimalOrZero("AD_CODTIPFRETE"));
        body.put("origin_zip_code", empresaVO.asString("CEP"));
        body.put("estimated_delivery_date", formatoData.format(previsaoentrega));
        JSONObject end_customer = new JSONObject();
        String Telefone = (parceiroVO.asString("TELEFONE") == null) ? "-" : parceiroVO.asString("TELEFONE");
        BigDecimal QtdCaracteres = new BigDecimal(parceiroVO.asString("NOMEPARC").length());
        end_customer.put("first_name", parceiroVO.asString("NOMEPARC").substring(0, parceiroVO.asString("NOMEPARC").indexOf(" ")));
        end_customer.put("last_name", parceiroVO.asString("NOMEPARC").substring(parceiroVO.asString("NOMEPARC").substring(0, parceiroVO.asString("NOMEPARC").indexOf(" ")).length()));
        end_customer.put("email", parceiroVO.asString("EMAIL"));
        end_customer.put("phone", formataDados(Telefone));
        end_customer.put("cellphone", formataDados(Telefone));
        end_customer.put("is_company", parceiroVO.asString("TIPPESSOA").equals("J"));
        end_customer.put("federal_tax_payer_id", parceiroVO.asString("CGC_CPF"));
        end_customer.put("state_tax_payer_id", parceiroVO.asString("IDENTINSCESTAD"));
        end_customer.put("shipping_country", "Brasil");
        end_customer.put("shipping_state", estadoVO.getProperty("UF"));
        end_customer.put("shipping_city", cidadeVO.getProperty("NOMECID"));
        end_customer.put("shipping_additional", parceiroVO.asString("COMPLEMENTO"));
        end_customer.put("shipping_address", enderecoVO.getProperty("NOMEEND"));
        end_customer.put("shipping_number", (parceiroVO.asString("NUMEND") == null) ? "SN" : parceiroVO.asString("NUMEND"));
        end_customer.put("shipping_quarter", bairroVO.getProperty("NOMEBAI"));
        end_customer.put("shipping_zip_code", parceiroVO.asString("CEP"));
        body.put("end_customer", end_customer);
        JSONArray shipment_order_volume_array = new JSONArray();
        NativeSql sql = (NativeSql)null;
        ResultSet volumesPedido = (ResultSet)null;
        sql = new NativeSql(jdbc);
        sql.appendSql("select\nlevel,\na.*\nfrom(\nselect\nnvl(cab.qtdvol,0) as qtdvol,\nsum(ite.vlrtot-ite.vlrdesc)/nvl(cab.qtdvol,0) as vlrtotitens,\nsum(ite.qtdneg*pro.pesobruto)/nvl(cab.qtdvol,0) as weight,\nsum(ite.qtdneg*pro.largura)/nvl(cab.qtdvol,0) as width,\nsum(ite.qtdneg*pro.altura)/nvl(cab.qtdvol,0) as height,\nsum(ite.qtdneg*pro.espessura)/nvl(cab.qtdvol,0) as length,\nsum(ite.qtdneg)/nvl(cab.qtdvol,0) as products_quantity,\ncab.ad_sigepcodrast as rastreio\nfrom tgfite ite\ninner join tgfcab cab on (ite.nunota = cab.nunota)\ninner join tgfpro pro on pro.codprod=ite.codprod\ninner join tgfvol vol on vol.codvol=ite.codvol\nwhere cab.NUNOTA=" + tgfCabVO
            
            .asBigDecimalOrZero("NUNOTA") + "\ngroup by\ncab.ad_sigepcodrast,\ncab.qtdvol\n)a\nconnect by level  <= A.QTDVOL");
        volumesPedido = sql.executeQuery();
        if (volumesPedido.next()) {
          JSONObject volume = new JSONObject();
          volume.put("name", "CAIXA");
          volume.put("shipment_order_volume_number", volumesPedido.getDouble("QTDVOL"));
          volume.put("volume_type_code", "BOX");
          volume.put("weight", volumesPedido.getDouble("weight"));
          volume.put("width", volumesPedido.getDouble("width"));
          volume.put("height", volumesPedido.getDouble("height"));
          volume.put("length", volumesPedido.getDouble("length"));
          volume.put("products_quantity", volumesPedido.getDouble("products_quantity"));
          volume.put("tracking_code", volumesPedido.getString("rastreio"));
          volume.put("products_nature", "PRODUTOS");
          JSONObject shipment_order_volume_invoice = new JSONObject();
          shipment_order_volume_invoice.put("invoice_series", tgfCabVO.asString("SERIENOTA"));
          shipment_order_volume_invoice.put("invoice_number", tgfCabVO.asBigDecimalOrZero("NUMNOTA"));
          shipment_order_volume_invoice.put("invoice_key", tgfCabVO.asString("CHAVENFE"));
          shipment_order_volume_invoice.put("invoice_date", formatoData.format(tgfCabVO.asTimestamp("DTFATUR")));
          shipment_order_volume_invoice.put("invoice_total_value", tgfCabVO.asBigDecimalOrZero("VLRNOTA"));
          shipment_order_volume_invoice.put("invoice_products_value", volumesPedido.getDouble("vlrtotitens"));
          JapeWrapper tgfTop = JapeFactory.dao("TipoOperacao");
          DynamicVO tgfTopVO = null;
          tgfTopVO = tgfTop.findByPK(new Object[] { tgfCabVO.asBigDecimal("CODTIPOPER"), tgfCabVO.asTimestamp("DHTIPOPER") });
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
        } 
        body.put("shipment_order_volume_array", shipment_order_volume_array);
        context.setMensagemRetorno(body.toString());
        try {
          CallService apiService = new CallService();
          apiService.setUri("https://api.intelipost.com.br/api/v1/shipment_order");
          apiService.setMethod("POST");
          apiService.setBody(body.toString());
          String response = apiService.fire();
          JSONObject json = new JSONObject(response);
          tgfCabVO.setProperty("AD_IDINTELIPOST", json.getJSONObject("content").get("id").toString());
          tgfCabVO.setProperty("DTPREVENT", previsaoentrega);
          tgfCabVO.setProperty("AD_DTENVPED", new Timestamp(System.currentTimeMillis()));
          rPLE.setValueObject(rEVO);
          String origem = (getOrigem(tgfCabVO.asBigDecimal("CODTIPOPER")) == null) ? new String("N") : getOrigem(tgfCabVO.asBigDecimal("CODTIPOPER"));
          if (origem.equals(new String("N"))) {
            String queryPk2 = "AD_PEDIDORIGEM=" + tgfCabVO.asBigDecimal("AD_PEDIDORIGEM");
            FinderWrapper f2 = new FinderWrapper("CabecalhoNota", queryPk2);
            Collection<PersistentLocalEntity> rPLES2 = dwf.findByDynamicFinder(f2);
            for (PersistentLocalEntity rPLE2 : rPLES2) {
              EntityVO rEVO2 = rPLE2.getValueObject();
              DynamicVO rVO2 = (DynamicVO)rEVO2;
              rVO2.setProperty("DTPREVENT", previsaoentrega);
              rVO2.setProperty("AD_DTENVPED", new Timestamp(System.currentTimeMillis()));
              rPLE2.setValueObject(rEVO2);
            } 
          } 
          JapeWrapper japeWrapper = JapeFactory.dao("AD_INTHIS");
          DynamicVO save = ((FluidCreateVO)japeWrapper.create().set("PEDIDO", tgfCabVO.asString("AD_PEDIDO"))).save();
          mensagem.append("<b>NF:</b> " + tgfCabVO.asBigDecimalOrZero("NUMNOTA") + " <b>Intelipost ID:</b> " + json.getJSONObject("content").get("id").toString() + "<br>");
        } catch (Exception exception) {}
      } 
      JapeWrapper empresaDAO = JapeFactory.dao("OrdemCarga");
      ((FluidUpdateVO)empresaDAO.prepareToUpdateByPK(new Object[] { linha.getCampo("CODEMP"), linha.getCampo("ORDEMCARGA") }).set("SITUACAO", new String("F")))
        .update();
      NativeSql insert = (NativeSql)null;
      insert = new NativeSql(jdbc);
      try {
        insert.appendSql("UPDATE AD_TGFORD SET STATUS='F' WHERE ORDEMCARGA=" + linha
            .getCampo("ORDEMCARGA") + " AND CODEMP=" + linha.getCampo("CODEMP"));
        insert.executeUpdate();
      } finally {
        NativeSql.releaseResources(insert);
      } 
    } 
    context.setMensagemRetorno(mensagem.toString());
  }
  
  public static String formataDados(String dado) {
    String dados = dado.replaceAll("[^0-9]+", "");
    return dados;
  }
  
  public static Integer ContarDados(String dado) {
    int dados = dado.length();
    return Integer.valueOf(dados);
  }
  
  private static BigDecimal getIntelipost(String id) throws Exception {
    JdbcWrapper jdbc = null;
    BigDecimal UltimaOrdem = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = null;
    ResultSet rs = null;
    String query = "SELECT\r\nNVL((\r\nSELECT \r\nNVL(PRAZOMINIMO,1) AS PRAZO\r\nFROM AD_SIMFRETENEG \r\nWHERE ID = '" + id + "'\r\nAND ATIVA = 'S'\r\n),1) AS PRAZO FROM DUAL\r\n";
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql(query);
      rs = sql.executeQuery();
      if (rs.next())
        UltimaOrdem = rs.getBigDecimal("PRAZO"); 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("SQL Error: " + e.getMessage() + " " + query);
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
    return UltimaOrdem;
  }
  
  private static BigDecimal getMagento(String id) throws Exception {
    JdbcWrapper jdbc = null;
    BigDecimal UltimaOrdem = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = null;
    ResultSet rs = null;
    String query = "SELECT\r\nNVL(SHIPPING_DAYS,0) AS PRAZO\r\nFROM AD_MAGPED\r\nWHERE ORDERID = '" + id + "'";
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql(query);
      rs = sql.executeQuery();
      if (rs.next())
        UltimaOrdem = rs.getBigDecimal("PRAZO"); 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("SQL Error: " + e.getMessage() + " " + query);
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
    return UltimaOrdem;
  }
  
  private static Timestamp getMagentoPrev(String id) throws Exception {
    JdbcWrapper jdbc = null;
    Timestamp UltimaOrdem = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = null;
    ResultSet rs = null;
    String query = "SELECT\r\nTO_DATE(PREVISAO_ENTREGA,'YYYY/MM/DD HH24:MI:SS') AS PREVISAO\r\nFROM AD_MAGPED\r\nWHERE ORDERID = '" + id + "'";
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql(query);
      rs = sql.executeQuery();
      if (rs.next())
        UltimaOrdem = rs.getTimestamp("PREVISAO"); 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("SQL Error: " + e.getMessage() + " " + query);
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
    return UltimaOrdem;
  }
  
  public static int getDiasTotais(Date start, int dias) throws Exception {
    int daysCount = dias;
    int prazo = dias;
    for (int i = 0; prazo >= 0; i++) {
      Calendar c1 = Calendar.getInstance();
      c1.setTime(start);
      c1.add(5, i);
      Calendar c2 = Calendar.getInstance();
      c2.setTime(start);
      c2.add(5, i);
      c2.set(1, 1900);
      if (c1.get(7) == 1 || c1
        .get(7) == 7 || 
        getFeriados(new Timestamp(c1.getTimeInMillis())) || 
        getFeriados(new Timestamp(c2.getTimeInMillis()))) {
        daysCount++;
      } else {
        prazo--;
      } 
    } 
    return daysCount;
  }
  
  public static boolean getFeriados(Timestamp data) throws Exception {
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = null;
    ResultSet rs = null;
    String query = "SELECT DTFERIADO FROM TSIFER WHERE NACIONAL = 'N' AND DTFERIADO='" + TimeUtils.clearTime(data).toLocalDateTime().format(FORMATTER) + "'";
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql(query);
      rs = sql.executeQuery();
      if (rs.next())
        return true; 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("SQL Error: " + e.getMessage() + " " + query);
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
    return false;
  }
  
  public static String getOrigem(BigDecimal codtipoper) throws Exception {
    JdbcWrapper jdbc = null;
    String retorno = new String("N");
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = null;
    ResultSet rs = null;
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql("SELECT NVL(SEMORIG,'N') AS RETORNO FROM AD_STAPED WHERE CODTIPOPER =" + codtipoper);
      rs = sql.executeQuery();
      if (rs.next())
        retorno = rs.getString("RETORNO"); 
    } catch (Exception e) {
      throw new Exception("SQL Error: " + e.getMessage());
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
    return retorno;
  }
  
  public static boolean semOrdemCarga(BigDecimal nunota) throws Exception {
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = (NativeSql)null;
    ResultSet rs = (ResultSet)null;
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql("SELECT NVL(ORDEMCARGA,0) AS ORDEMCARGA FROM TGFCAB WHERE NUNOTA =" + nunota);
      rs = sql.executeQuery();
      if (rs.next() && rs
        .getBigDecimal("ORDEMCARGA").equals(new BigDecimal(0)))
        return true; 
    } catch (Exception e) {
      throw new Exception("SQL Error: " + e.getMessage());
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
    return false;
  }
}
