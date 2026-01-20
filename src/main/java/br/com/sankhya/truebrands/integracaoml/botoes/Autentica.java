package br.com.sankhya.truebrands.integracaoml.botoes;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.truebrands.integracaoml.ApiClient;

import java.math.BigDecimal;

public class Autentica implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();
        if(linhas.length > 1){
            throw new Exception("Favor selecionar apenas uma linha");
        }

        Registro linha = linhas[0];
        BigDecimal codEmp = (BigDecimal) linha.getCampo("CODEMP");

        ApiClient api = new ApiClient(codEmp);
        api.autenticacao();

    }
}
