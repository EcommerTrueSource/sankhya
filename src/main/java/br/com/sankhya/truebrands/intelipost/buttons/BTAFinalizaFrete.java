package br.com.sankhya.truebrands.intelipost.buttons;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.truebrands.utils.Utils;

import java.math.BigDecimal;

public class BTAFinalizaFrete implements AcaoRotinaJava {
  public void doAction(ContextoAcao context) throws Exception {
    Registro[] registros = context.getLinhas();
    Registro linha = registros[0];
    if (registros.length > 1)
      context.mostraErro("Selecione apenas um registro!"); 
    if (linha.getCampo("PENDENTE").equals(new String("N")) && linha
      .getCampo("TIPMOV").equals(new String("P")))
      throw new Exception("Pedido faturado npode ter frete alterado!"); 
    if (!linha.getCampo("TIPMOV").equals(new String("P")) && 
      Utils.existeOrigem((BigDecimal)linha.getCampo("NUNOTA")))
      throw new Exception("Frete spode ser alterado no pedido!"); 
    if (!linha.getCampo("TIPMOV").equals(new String("P")) && 
      !Utils.existeOrigem((BigDecimal)linha.getCampo("NUNOTA")) && linha
      .getCampo("STATUSNOTA").equals(new String("L")))
      throw new Exception("Frete spode ser alterado em Nota Fiscal nconfirmada!"); 
    context.setMensagemRetorno("Frete definido com sucesso!");
  }
}
