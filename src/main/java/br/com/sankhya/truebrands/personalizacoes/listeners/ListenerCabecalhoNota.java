package br.com.sankhya.truebrands.personalizacoes.listeners;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.comercial.ComercialUtils;

import java.math.BigDecimal;

public class ListenerCabecalhoNota implements EventoProgramavelJava {
    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        System.out.println("(Dev-True) entrou no evento da cab." );
        DynamicVO cabVO = (DynamicVO) event.getVo();
        alteraEnderecoParceiro(cabVO);
    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeCommit(TransactionContext tranCtx) throws Exception {

    }

    private void alteraEnderecoParceiro(DynamicVO cabVO) throws Exception{
        System.out.println("(Dev-True) ListenerCab. TOP: "+cabVO.asBigDecimalOrZero("CODTIPOPER"));
        DynamicVO top = ComercialUtils.getTipoOperacao(cabVO.asBigDecimalOrZero("CODTIPOPER"));
        boolean alteraParceiro = "S".equals(top.asString("AD_ALTERAENDPAR"));
        System.out.println("(Dev-True) ListenerCab. TOP: "+alteraParceiro);
        if(alteraParceiro){
            try{
                    System.out.println("(Dev-True) ListenerCab. Try");
                JapeWrapper parDAO = JapeFactory.dao("Parceiro");
                BigDecimal codParc = cabVO.asBigDecimalOrZero("CODPARC");
                BigDecimal codContato = cabVO.asBigDecimalOrZero("CODCONTATO");

                System.out.println("(Dev-True) ListenerCab. Try Contato: "+codContato);
                System.out.println("(Dev-True) ListenerCab. Try Parceiro: "+codParc);

                DynamicVO contatoVO = JapeFactory.dao("Contato").findByPK(new Object[]{codParc,codContato});
                DynamicVO parVO = parDAO.findByPK(codParc);




                FluidUpdateVO fluidUpdateVO = parDAO.prepareToUpdateByPK(codParc);

                if(parVO.asString("AD_BCKPCEP") == null){
                    fluidUpdateVO.set("AD_BCKPCEP",parVO.asString("CEP"))
                            .set("AD_BCKPCODEND",parVO.asBigDecimalOrZero("CODEND"))
                            .set("AD_BCKPNUMEND",parVO.asString("NUMEND"))
                            .set("AD_BACKPCOMP",parVO.asString("COMPLEMENTO"))
                            .set("AD_BCKPCODBAI",parVO.asBigDecimalOrZero("CODBAI"))
                            .set("AD_BCKPCODCID",parVO.asBigDecimalOrZero("CODCID"));
                }

                fluidUpdateVO.set("CEP",contatoVO.asString("CEP"))
                        .set("CODEND",contatoVO.asBigDecimalOrZero("CODEND"))
                        .set("NUMEND",contatoVO.asString("NUMEND"))
                        .set("COMPLEMENTO",contatoVO.asString("COMPLEMENTO"))
                        .set("CODBAI",contatoVO.asBigDecimalOrZero("CODBAI"))
                        .set("CODCID",contatoVO.asBigDecimalOrZero("CODCID")).update();
                System.out.println("(Dev-True) deu UPDATE no ListenerCab. Try");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("(Dev-True) deu Erro no ListenerCab." +e.getMessage());
                throw e;
            }

        }

    }
}
