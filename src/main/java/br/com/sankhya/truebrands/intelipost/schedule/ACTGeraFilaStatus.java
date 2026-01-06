package br.com.sankhya.truebrands.intelipost.schedule;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

public class ACTGeraFilaStatus implements ScheduledAction {
  public void onTime(ScheduledActionContext context) {
    context.log("PROCESSO: ATUALIZSTATUS");
    JapeSession.SessionHandle hnd = null;
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = (NativeSql)null;
    ResultSet rs = (ResultSet)null;
    try {
      JapeWrapper empresaDAO3 = JapeFactory.dao("AD_ADINTELFILA");
      empresaDAO3.deleteByCriteria("", new Object[0]);
    } catch (Exception e1) {
      e1.printStackTrace();
    } 
    try {
      hnd = JapeSession.open();
      sql = new NativeSql(jdbc);
      sql.appendSql("SELECT\r\nCAB.AD_PEDIDO AS PEDIDO,\r\nCAB.NUNOTA AS NUNOTA,\r\nCAB.AD_PEDIDORIGEM AS ORIGEM\r\nFROM TGFCAB CAB\r\nWHERE CAB.AD_IDINTELIPOST IS NOT NULL\r\nAND CAB.TIPMOV = 'V'\r\nAND CAB.DTMOV >= SYSDATE-30\r\nAND NVL(CAB.AD_STATUSENTREGA,'PENDENTE') <> 'Entregue' \r\n");
      rs = sql.executeQuery();
      while (rs.next()) {
        Date dateobj = new Date();
        JapeWrapper empresaDAO = JapeFactory.dao("AD_ADINTELFILA");
        DynamicVO dynamicVO = ((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)empresaDAO.create().set("PEDIDO", rs.getString("PEDIDO"))).set("NUNOTAORIG", rs.getBigDecimal("ORIGEM"))).set("NUNOTA", rs.getBigDecimal("NUNOTA"))).set("STATUS", "P")).set("DHALTER", new Timestamp(dateobj.getTime()))).set("ORIGEM", "br.com.devstudios.intelipost.eventos.ACTGeraFilaStatus")).save();
      } 
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      JapeSession.close(hnd);
    } 
  }
}
