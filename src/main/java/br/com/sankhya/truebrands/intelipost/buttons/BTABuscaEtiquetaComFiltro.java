package br.com.sankhya.truebrands.intelipost.buttons;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.truebrands.intelipost.IntelipostService;
import org.json.JSONArray;
import org.json.JSONObject;

public class BTABuscaEtiquetaComFiltro implements AcaoRotinaJava {
  public void doAction(ContextoAcao context) throws Exception {
    Registro[] registros = context.getLinhas();
    Registro linha = registros[0];
    String numeroPedido = linha.getCampo("NUNOTA").toString();
    try {
      IntelipostService intelipostService = new IntelipostService();
      JSONObject pedido = intelipostService.consultarPedido(numeroPedido);
      if (pedido != null) {
        JSONArray shipment_order_volume_array = pedido.getJSONObject("content").getJSONArray("shipment_order_volume_array");
        JSONObject shipment_order_volume_invoice = shipment_order_volume_array.getJSONObject(0).getJSONObject("shipment_order_volume_invoice");
        String numeroInvoice = shipment_order_volume_invoice.getString("shipment_order_volume_number");
        JSONObject etiqueta = intelipostService.getEtiqueta(numeroPedido, numeroInvoice);
        if (etiqueta != null) {
          String linkEtiqueta = etiqueta.getJSONObject("content").getString("label_url");
          linha.setCampo("AD_LINKETIQUE", linkEtiqueta);
          StringBuilder sb = new StringBuilder();
          sb.append("<div style=\"text-align: center; padding: 20px; font-family: sans-serif;\">");
          sb.append("<h2>Etiqueta Gerada!</h2>");
          sb.append("<p>Sua etiqueta de envio estpronta para impress");
          sb.append("<p>Clique no botabaixo para abrir a etiqueta em uma nova aba.</p>");
          sb.append("<br>");
          sb.append("<a href=\"").append(linkEtiqueta).append("\" target=\"_blank\" ");
          sb.append("style=\"display: inline-block; padding: 12px 25px; background-color: #28a745; color: white; text-align: center; text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold; border: none; cursor: pointer;\">");
          sb.append("&#128424; Imprimir Etiqueta");
          sb.append("</a>");
          sb.append("</div>");
          context.setMensagemRetorno(sb.toString());
        } else {
          throw new Exception("Nao foi possivel consultar a etiqueta");
        } 
      } else {
        throw new Exception("Nao foi possivel consultar o pedido");
      } 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    } 
  }
}
