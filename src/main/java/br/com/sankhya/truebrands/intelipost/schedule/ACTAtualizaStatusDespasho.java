package br.com.sankhya.truebrands.intelipost.schedule;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truebrands.intelipost.IntelipostService;
import com.sankhya.util.JdbcUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import org.json.JSONArray;
import org.json.JSONObject;

public class ACTAtualizaStatusDespasho implements ScheduledAction {
  public void onTime(ScheduledActionContext context) {
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    AuthenticationInfo auth = new AuthenticationInfo("SUP", BigDecimal.ZERO, BigDecimal.ZERO, Integer.valueOf(0));
    auth.makeCurrent();
    JapeSessionContext.putProperty("usuario_logado", BigDecimal.ZERO);
    NativeSql sql = (NativeSql)null;
    ResultSet rs = (ResultSet)null;
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql("SELECT\n    CAB.NUNOTA\nFROM TGFCAB CAB\nJOIN TGFORD ORD ON ORD.ORDEMCARGA = CAB.ORDEMCARGA\nWHERE CAB.AD_STATUSPED = 'PE'\n  AND CAB.AD_STATUSINTELIPOST = 'C'\n  AND ORD.SITUACAO = 'F'\n  AND CAB.DTNEG >= SYSDATE - 7\n  AND CAB.AD_IDINTELIPOST IS NOT NULL\nORDER BY DBMS_RANDOM.VALUE\nFETCH FIRST 50 ROWS ONLY");
      rs = sql.executeQuery();
      JSONArray pedidos = new JSONArray();
      List<BigDecimal> nunotas = new ArrayList<>();
      while (rs.next()) {
        BigDecimal order_number = rs.getBigDecimal("NUNOTA");
        String event_date = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")).format(new Date());
        pedidos.put((new JSONObject()).put("order_number", order_number).put("event_date", event_date));
        nunotas.add(order_number);
      } 
      if (pedidos.length() > 0)
        try {
          IntelipostService intelipostService = new IntelipostService();
          intelipostService.marcarPedidosComoExpedidosComData(pedidos);
          JapeWrapper empresaDAO2 = JapeFactory.dao("CabecalhoNota");
          for (BigDecimal nunota : nunotas) {
            ((FluidUpdateVO)empresaDAO2.prepareToUpdateByPK(new Object[] { nunota }).set("AD_STATUSPED", "D"))
              .update();
          } 
        } catch (Exception e) {
          e.printStackTrace();
        }  
    } catch (Exception e1) {
      e1.printStackTrace();
    } finally {
      JdbcUtils.closeResultSet(rs);
      NativeSql.releaseResources(sql);
    } 
  }
}
