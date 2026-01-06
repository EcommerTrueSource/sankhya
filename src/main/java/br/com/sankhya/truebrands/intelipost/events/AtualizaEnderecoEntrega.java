package br.com.sankhya.truebrands.intelipost.events;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class AtualizaEnderecoEntrega implements EventoProgramavelJava {
  public void afterDelete(PersistenceEvent arg0) throws Exception {}
  
  public void afterInsert(PersistenceEvent event) throws Exception {}
  
  public void afterUpdate(PersistenceEvent arg0) throws Exception {}
  
  public void beforeCommit(TransactionContext arg0) throws Exception {}
  
  public void beforeDelete(PersistenceEvent arg0) throws Exception {}
  
  public void beforeInsert(PersistenceEvent arg0) throws Exception {}
  
  public void beforeUpdate(PersistenceEvent event) throws Exception {
    JapeWrapper tgfPar = JapeFactory.dao("Parceiro");
    JapeSession.SessionHandle hnd = null;
    DynamicVO tgfParVO = null;
    DynamicVO nota = null;
    DynamicVO notaOld = null;
    try {
      nota = (DynamicVO)event.getVo();
      notaOld = (DynamicVO)event.getOldVO();
      if (nota.asString("CGC_CPF").length() != 0 && notaOld
        .asString("CGC_CPF") == null) {
        tgfParVO = tgfPar.findOne("CGC_CPF = ?", new Object[] { nota.asString("CGC_CPF") });
        nota.setProperty("CODPARC", tgfParVO.asBigDecimal("CODPARC"));
        nota.setProperty("CODEND", tgfParVO.asBigDecimal("CODEND"));
        nota.setProperty("NUMEND", tgfParVO.asString("NUMEND"));
        nota.setProperty("CODBAI", tgfParVO.asBigDecimal("CODBAI"));
        nota.setProperty("COMPLEMENTO", tgfParVO.asString("COMPLEMENTO"));
        nota.setProperty("CODCID", tgfParVO.asBigDecimal("CODCID"));
        nota.setProperty("CODREG", tgfParVO.asBigDecimal("CODREG"));
        nota.setProperty("CEP", tgfParVO.asString("CEP"));
      } 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    } finally {
      JapeSession.close(hnd);
    } 
  }
}
