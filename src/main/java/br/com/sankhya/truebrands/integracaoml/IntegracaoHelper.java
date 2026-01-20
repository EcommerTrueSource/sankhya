package br.com.sankhya.truebrands.integracaoml;

import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import com.sankhya.util.TimeUtils;

import java.nio.charset.StandardCharsets;

public class IntegracaoHelper {
    static public void salvaLog(String response,String request) throws Exception {
        salvaLogInterno(response,request,null);
    }

    static public void salvaLog(String response,String request,String pedido) throws Exception {
        salvaLogInterno(response,request,pedido);
    }

    static private void salvaLogInterno(String response,String request,String pedido) throws Exception {
        JapeWrapper logDAO = JapeFactory.dao("AD_LOGINTEGRACAO");
        logDAO.create().set("DHINS", TimeUtils.getNow())
                .set("REQUEST",request)
                .set("NUMPEDIDO",pedido)
                .set("RESPONSE",response.toCharArray())
                .save();
    }
}
