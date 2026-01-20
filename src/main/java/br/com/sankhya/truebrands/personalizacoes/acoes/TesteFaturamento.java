package br.com.sankhya.truebrands.personalizacoes.acoes;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.truebrands.personalizacoes.callback.ProcessamentosAposConfirmacao;

import java.math.BigDecimal;

public class TesteFaturamento implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro linha = contexto.getLinhas()[0];
        BigDecimal nuNota = (BigDecimal) linha.getCampo("NUNOTA");
        ProcessamentosAposConfirmacao.faturaPedido(nuNota);
    }
}
