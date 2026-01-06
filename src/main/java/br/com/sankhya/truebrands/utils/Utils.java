package br.com.sankhya.truebrands.utils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truebrands.intelipost.CallService;
import com.sankhya.util.JdbcUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

public class Utils {
    public static String getStatus(Object pedido) throws Exception {
        String retorno = null;
        try {
            CallService apiService = new CallService();
            apiService.setUri("https://api.intelipost.com.br/api/v1/shipment_order/invoice_key/" + pedido);
            apiService.setMethod("GET");
            String response = apiService.fire();
            JSONObject json = new JSONObject(response);
            JSONArray content = json.getJSONArray("content");
            for (int a = 0; a < content.length(); a++) {
                JSONObject entrega = content.getJSONObject(a);
                JSONArray shipment = entrega.getJSONArray("shipment_order_volume_array");
                for (int b = 0; b < shipment.length(); b++) {
                    JSONObject entregafinal = shipment.getJSONObject(b);
                    String status = (String)entregafinal.get("shipment_order_volume_state_localized");
                    retorno = status;
                }
            }
        } catch (Exception e) {
            throw new Exception("SQL Error: " + e.getMessage());
        } finally {}
        return retorno;
    }

    public static String getDataEntrega(Object pedido) throws Exception {
        String retorno = null;
        try {
            CallService apiService = new CallService();
            apiService.setUri("https://api.intelipost.com.br/api/v1/shipment_order/invoice_key/" + pedido);
            apiService.setMethod("GET");
            String response = apiService.fire();
            JSONObject json = new JSONObject(response);
            JSONArray content = json.getJSONArray("content");
            for (int a = 0; a < content.length(); a++) {
                JSONObject entrega = content.getJSONObject(a);
                JSONArray shipment = entrega.getJSONArray("shipment_order_volume_array");
                for (int b = 0; b < shipment.length(); b++) {
                    JSONObject entregafinal = shipment.getJSONObject(b);
                    String status = (String)entregafinal.get("delivered_date_iso");
                    retorno = status;
                }
            }
        } catch (Exception e) {
            throw new Exception("SQL Error: " + e.getMessage());
        } finally {}
        return retorno;
    }

    public static String getTimestamp(String data) throws Exception {
        String datafinal = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = sdf.parse(data);
            datafinal = output.format(d);
        } catch (Exception e) {
            throw new Exception("SQL Error: " + e.getMessage());
        } finally {}
        return datafinal;
    }

    public static boolean existeLinha(Object pedido) throws Exception {
        JdbcWrapper jdbc = null;
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        jdbc = dwfEntityFacade.getJdbcWrapper();
        NativeSql sql = (NativeSql)null;
        ResultSet rs = (ResultSet)null;
        try {
            sql = new NativeSql(jdbc);
            sql.appendSql("SELECT 1 FROM AD_INTHISENT WHERE PEDIDO =" + pedido);
            rs = sql.executeQuery();
            if (rs.next())
                return true;
        } catch (Exception e) {
            throw new Exception("SQL Error: " + e.getMessage());
        } finally {
            JdbcUtils.closeResultSet(rs);
            NativeSql.releaseResources(sql);
        }
        return false;
    }

    public static boolean existeOrigem(BigDecimal pedido) throws Exception {
        JdbcWrapper jdbc = null;
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        jdbc = dwfEntityFacade.getJdbcWrapper();
        NativeSql sql = (NativeSql)null;
        ResultSet rs = (ResultSet)null;
        try {
            sql = new NativeSql(jdbc);
            sql.appendSql("SELECT \r\n1\r\nFROM TGFVAR VAR\r\nINNER JOIN TGFCAB CAB ON (VAR.NUNOTAORIG = CAB.NUNOTA)\r\nWHERE VAR.NUNOTA = " + pedido + "\r\nAND CAB.TIPMOV = 'P'");
            rs = sql.executeQuery();
            if (rs.next())
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

