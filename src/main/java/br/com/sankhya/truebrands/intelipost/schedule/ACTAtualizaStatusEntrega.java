package br.com.sankhya.truebrands.intelipost.schedule;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truebrands.utils.Utils;
import com.sankhya.util.JdbcUtils;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

public class ACTAtualizaStatusEntrega implements ScheduledAction {
  public void onTime(ScheduledActionContext context) {
    context.log("PROCESSO: ATUALIZA_STATUS_INTELIPOST");
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = (NativeSql)null;
    ResultSet rs = (ResultSet)null;
    NativeSql sqlProduto = (NativeSql)null;
    ResultSet rsProduto = (ResultSet)null;
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql("SELECT FIL.PEDIDO,FIL.ID,FIL.NUNOTA,FIL.NUNOTAORIG,CAB.CHAVENFE FROM AD_ADINTELFILA FIL\r\nINNER JOIN TGFCAB CAB ON (CAB.NUNOTA = FIL.NUNOTA)\r\nWHERE FIL.STATUS IN ('P') order by dbms_random.value FETCH FIRST 5 ROWS ONLY");
      rs = sql.executeQuery();
      while (rs.next()) {
        NativeSql update = (NativeSql)null;
        update = new NativeSql(jdbc);
        try {
          String status = Utils.getStatus(rs.getString("CHAVENFE"));
          String dataEntrega = null;
          if (status.equals(new String("Entregue")))
            dataEntrega = Utils.getTimestamp(Utils.getDataEntrega(rs.getString("CHAVENFE"))); 
          JapeWrapper empresaDAO = JapeFactory.dao("CabecalhoNota");
          ((FluidUpdateVO)((FluidUpdateVO)empresaDAO.prepareToUpdateByPK(new Object[] { rs.getBigDecimal("NUNOTA") }).set("AD_STATUSENTREGA", status))
            .set("AD_DTENTREGA", (dataEntrega == null) ? null : Timestamp.valueOf(dataEntrega)))
            .update();
          JapeWrapper empresaDAO2 = JapeFactory.dao("CabecalhoNota");
          ((FluidUpdateVO)((FluidUpdateVO)empresaDAO2.prepareToUpdateByPK(new Object[] { rs.getBigDecimal("NUNOTAORIG") }).set("AD_STATUSENTREGA", status))
            .set("AD_DTENTREGA", (dataEntrega == null) ? null : Timestamp.valueOf(dataEntrega)))
            .update();
          if (status.equals(new String("Entregue"))) {
            NativeSql insert = (NativeSql)null;
            insert = new NativeSql(jdbc);
            try {
              insert.appendSql("UPDATE TGFCAB SET AD_STATUS=" + new String("10") + ", AD_SEQSTATUS=AD_SEQSTATUS+1 WHERE AD_PEDIDORIGEM=" + rs
                  .getBigDecimal("NUNOTAORIG"));
              insert.executeUpdate();
            } finally {
              NativeSql.releaseResources(insert);
            } 
          } 
          update.appendSql("UPDATE AD_ADINTELFILA SET status='C' where id=" + rs.getBigDecimal("ID"));
          update.executeUpdate();
        } catch (Exception e) {
          context.log("PROCESSO: ERRSINCSTATINT: " + e.getMessage());
          update.appendSql("UPDATE AD_ADINTELFILA SET status='E', erro='" + e.getMessage() + "' where id=" + rs.getBigDecimal("ID"));
          update.executeUpdate();
        } finally {
          NativeSql.releaseResources(update);
        } 
      } 
    } catch (Exception e1) {
      e1.printStackTrace();
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
      JdbcUtils.closeResultSet(rsProduto);
      NativeSql.releaseResources(sqlProduto);
    } 
  }
}
