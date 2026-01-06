package br.com.sankhya.truebrands.intelipost.buttons;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truebrands.utils.Utils;
import com.sankhya.util.JdbcUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;

public class BTADefineFrete implements AcaoRotinaJava {
  public void doAction(ContextoAcao context) throws Exception {
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    Registro[] registros = context.getLinhas();
    if (registros.length > 1)
      context.mostraErro("Selecione apenas um registro!"); 
    Registro linha = registros[0];
    BigDecimal nunota = buscaNunota((String)linha.getCampo("ID"));
    PersistentLocalEntity order = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", nunota);
    EntityVO orderEVO = order.getValueObject();
    DynamicVO orderVO = (DynamicVO)orderEVO;
    if (orderVO.asString("PENDENTE").equals(new String("N")) && orderVO
      .asString("TIPMOV").equals(new String("P")))
      throw new Exception("Pedido faturado npode ter frete alterado!"); 
    if (!orderVO.asString("TIPMOV").equals(new String("P")) && 
      Utils.existeOrigem(orderVO.asBigDecimal("NUNOTA")))
      throw new Exception("Frete spode ser alterado no pedido!"); 
    if (!orderVO.asString("TIPMOV").equals(new String("P")) && 
      !Utils.existeOrigem(orderVO.asBigDecimal("NUNOTA")) && orderVO
      .asString("STATUSNOTA").equals(new String("L")))
      throw new Exception("Frete spode ser alterado em Nota Fiscal nconfirmada!"); 
    orderVO.setProperty("VLRDESTAQUE", linha.getCampo("FRETETOTAL"));
    orderVO.setProperty("TIPFRETE", new String("N"));
    orderVO.setProperty("VLRFRETE", linha.getCampo("CUSTOREAL"));
    orderVO.setProperty("CODPARCTRANSP", linha.getCampo("CODPARC"));
    order.setValueObject(orderEVO);
    linha.setCampo("ATIVA", "S");
    mudaStatus(linha);
    ImpostosHelpper helpper = new ImpostosHelpper();
    helpper.setForcarRecalculo(true);
    helpper.calcularImpostos(nunota);
    helpper.totalizarNota(nunota);
    context.setMensagemRetorno("Frete definido com sucesso!");
  }
  
  private void mudaStatus(Registro linha) throws Exception {
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = (NativeSql)null;
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql("UPDATE AD_SIMFRETENEG SET ATIVA='N' where id='" + linha.getCampo("ID") + "' and SEQUENCIA <> " + linha.getCampo("SEQUENCIA"));
      sql.executeUpdate();
    } catch (Exception e) {
      throw new Exception("SQL Error: " + e.getMessage());
    } finally {
      NativeSql.releaseResources(sql);
    } 
  }
  
  private static BigDecimal buscaNunota(String id) throws Exception {
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = (NativeSql)null;
    ResultSet rs = (ResultSet)null;
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql("select NUNOTA from AD_SIMFRETE where id='" + id + "'");
      rs = sql.executeQuery();
      if (rs.next())
        return rs.getBigDecimal("NUNOTA"); 
    } catch (Exception e) {
      throw new Exception("SQL Error: " + e.getMessage());
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
    return BigDecimal.ZERO;
  }
}
