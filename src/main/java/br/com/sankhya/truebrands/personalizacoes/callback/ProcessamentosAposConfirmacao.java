package br.com.sankhya.truebrands.personalizacoes.callback;

import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.PrePersistEntityState;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.mgecomercial.model.facades.helpper.FaturamentoHelper;
import br.com.sankhya.modelcore.comercial.CentralFaturamento;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.custommodule.ICustomCallBack;
import br.com.sankhya.ws.ServiceContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ProcessamentosAposConfirmacao implements ICustomCallBack {
    @Override
    public Object call(String id, Map<String, Object> data) {

        Object ppe = data.get("cab_state");
        if(ppe != null){
            PrePersistEntityState prePersistEntityState = (PrePersistEntityState) ppe;
            DynamicVO cabVO = prePersistEntityState.getNewVO();
            BigDecimal nuNota = cabVO.asBigDecimal("NUNOTA");
            if("S".equals(cabVO.asString("TipoOperacao.AD_FATURAAUTOMATICO"))){
                BigDecimal nuNotaFaturada  = faturaPedido(nuNota);
                try {
                    confirmaFaturamento(nuNotaFaturada);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if("S".equals(cabVO.asString("TipoOperacao.AD_ALTERAENDPAR"))){
                voltaEndParceiro(cabVO.asDymamicVO("Parceiro"));
            }

        }
        return null;
    }

    static private void confirmaFaturamento(BigDecimal nuNota) throws Exception {
        try {
            CACHelper cacHelper = new CACHelper();
            cacHelper.processarConfirmacao(nuNota);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Dev - Erro ao confirmar "+e.getMessage());
            throw new RuntimeException(e);
        }

    }

    static public BigDecimal faturaPedido(BigDecimal nuNota) {
        JapeSession.SessionHandle hnd = null;
        try{
            System.out.println("Entrou no try callback");
            Collection<BigDecimal> nunotas = new ArrayList<>();
            nunotas.add(nuNota);
            hnd = JapeSession.open();
            ServiceContext ctx = ServiceContext.getCurrent();
            CentralFaturamento.ConfiguracaoFaturamento cfg = new CentralFaturamento.ConfiguracaoFaturamento();

            cfg.setAtualizarQtdAtendida(true);
            cfg.setSerie("1");
            cfg.setUsaTopDestino(true);
            Collection<BigDecimal> arrNunota = FaturamentoHelper.faturarInterno(ctx,hnd,cfg,nunotas,null);
            return arrNunota.iterator().next();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("(Dev-True) deu Erro no faturamento." +e.getMessage());
            throw new RuntimeException(e);
        }


    }

    private void voltaEndParceiro(DynamicVO parVO){
        JapeSession.SessionHandle hnd = null;
        try{
            hnd = JapeSession.open();
            hnd.execEnsuringTX(() -> {
                JapeWrapper parDAO = JapeFactory.dao("Parceiro");
                FluidUpdateVO fluidUpdateVO = parDAO.prepareToUpdate(parVO);

                fluidUpdateVO.set("CEP",parVO.asString("AD_BCKPCEP"))
                        .set("CODEND",parVO.asBigDecimalOrZero("AD_BCKPCODEND"))
                        .set("NUMEND",parVO.asString("AD_BCKPNUMEND"))
                        .set("COMPLEMENTO",parVO.asString("AD_BACKPCOMP"))
                        .set("CODBAI",parVO.asBigDecimalOrZero("AD_BCKPCODBAI"))
                        .set("CODCID",parVO.asBigDecimalOrZero("AD_BCKPCODCID")).update();
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("(Dev-True) deu Erro ao voltar com endereço do parceiro." +e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
