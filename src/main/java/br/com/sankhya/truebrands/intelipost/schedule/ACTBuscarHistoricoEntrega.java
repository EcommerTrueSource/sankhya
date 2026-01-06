package br.com.sankhya.truebrands.intelipost.schedule;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truebrands.intelipost.CallService;
import br.com.sankhya.truebrands.utils.Utils;
import com.sankhya.util.JdbcUtils;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import org.json.JSONArray;
import org.json.JSONObject;

public class ACTBuscarHistoricoEntrega implements ScheduledAction {
  public void onTime(ScheduledActionContext context) {
    context.log("PROCESSO: ATUALIZA_HISTORICO_ENTREGA_INTELIPOST");
    JapeSession.SessionHandle hnd = null;
    JdbcWrapper jdbc = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbc = dwfEntityFacade.getJdbcWrapper();
    NativeSql sql = (NativeSql)null;
    ResultSet rs = (ResultSet)null;
    NativeSql sqlProduto = (NativeSql)null;
    ResultSet rsProduto = (ResultSet)null;
    try {
      sql = new NativeSql(jdbc);
      sql.appendSql("SELECT PEDIDO,NUNOTA FROM AD_INTHIS HIS INNER JOIN TGFCAB CAB ON (HIS.PEDIDO = CAB.AD_PEDIDO AND CAB.TIPMOV = 'V') AND TRANSPORTADORA IS NULL order by dbms_random.value FETCH FIRST 5 ROWS ONLY");
      rs = sql.executeQuery();
      while (rs.next()) {
        NativeSql update = (NativeSql)null;
        update = new NativeSql(jdbc);
        try {
          hnd = JapeSession.open();
          CallService apiService = new CallService();
          apiService.setUri("https://api.intelipost.com.br/api/v1/shipment_order/" + rs.getString("PEDIDO"));
          apiService.setMethod("GET");
          String response = apiService.fire();
          JSONObject json = new JSONObject(response);
          if (Utils.existeLinha(rs.getString("PEDIDO"))) {
            JapeWrapper japeWrapper = JapeFactory.dao("AD_INTHISENT");
            japeWrapper.deleteByCriteria("PEDIDO = ?", new Object[] { rs.getString("PEDIDO") });
          } 
          JSONObject content = json.getJSONObject("content");
          JSONObject endereco = content.getJSONObject("end_customer");
          JSONArray shipment = content.getJSONArray("shipment_order_volume_array");
          JapeWrapper empresaDAO = JapeFactory.dao("AD_INTHIS");
          ((FluidUpdateVO)((FluidUpdateVO)((FluidUpdateVO)empresaDAO.prepareToUpdateByPK(new Object[] { rs.getString("PEDIDO") }).set("PREVENT", content.has("estimated_delivery_date_iso") ? Timestamp.valueOf(Utils.getTimestamp(content.getString("estimated_delivery_date_iso"))) : ""))
            .set("CGC_CPF", endereco.get("federal_tax_payer_id")))
            .set("TRANSPORTADORA", content.get("delivery_method_name")))
            .update();
          for (int c = 0; c < shipment.length(); c++) {
            JSONObject entrega = shipment.getJSONObject(c);
            JapeWrapper empresaDAO2 = JapeFactory.dao("AD_INTHIS");
            ((FluidUpdateVO)empresaDAO2.prepareToUpdateByPK(new Object[] { rs.getString("PEDIDO") }).set("STATUSENTREGA", entrega.get("shipment_order_volume_state_localized")))
              .update();
            JapeWrapper empresaDAO4 = JapeFactory.dao("CabecalhoNota");
            ((FluidUpdateVO)empresaDAO4.prepareToUpdateByPK(new Object[] { rs.getBigDecimal("NUNOTA") }).set("AD_STATUSENTREGA", entrega.get("shipment_order_volume_state_localized")))
              .update();
          } 
          for (int a = 0; a < shipment.length(); a++) {
            JSONObject array = shipment.getJSONObject(a);
            JSONArray history = array.getJSONArray("shipment_order_volume_state_history_array");
            for (int b = history.length() - 1; b >= 0; b--) {
              JSONObject entrega = history.getJSONObject(b);
              JSONObject state = entrega.getJSONObject("shipment_volume_micro_state");
              int total = (state.get("description").toString().length() > 100) ? 100 : state.get("description").toString().length();
              JapeWrapper empresaDAO3 = JapeFactory.dao("AD_INTHISENT");
              DynamicVO save = ((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)empresaDAO3.create().set("PEDIDO", rs.getString("PEDIDO"))).set("EVENTO", state.get("shipment_volume_state_localized").toString())).set("DESCRICAO", state.get("description").toString().substring(0, total))).set("DETALHE", entrega.get("provider_message").equals(null) ? "" : entrega.get("provider_message"))).set("DATA", Timestamp.valueOf(Utils.getTimestamp(entrega.getString("event_date_iso"))))).save();
              if (state.get("shipment_volume_state_localized").toString().equals(new String("Entregue"))) {
                JapeWrapper empresaDAO5 = JapeFactory.dao("AD_INTHIS");
                ((FluidUpdateVO)empresaDAO5.prepareToUpdateByPK(new Object[] { rs.getString("PEDIDO") }).set("DTENTREGA", Timestamp.valueOf(Utils.getTimestamp(entrega.getString("event_date_iso")))))
                  .update();
              } 
            } 
          } 
        } catch (Exception e) {
          context.log("PROCESSO: ERRSINCSTATINT: " + e.getMessage());
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
