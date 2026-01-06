package br.com.sankhya.truebrands.intelipost.events;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class EvtTgfcabIntelipost implements EventoProgramavelJava {
  public void afterInsert(PersistenceEvent event) throws Exception {}
  
  public void afterUpdate(PersistenceEvent event) throws Exception {}
  
  public void beforeUpdate(PersistenceEvent event) throws Exception {}
  
  public void afterDelete(PersistenceEvent event) throws Exception {
    JapeSession.SessionHandle hnd = null;
    DynamicVO tgfCabVO = null;
    try {
      hnd = JapeSession.open();
      tgfCabVO = (DynamicVO)event.getVo();
      if (tgfCabVO.asString("AD_IDSIMFRETE") != null) {
        JapeWrapper itemNotaDAO1 = JapeFactory.dao("AD_SIMFRETENEG");
        itemNotaDAO1.deleteByCriteria("ID = ?", new Object[] { tgfCabVO.asString("AD_IDSIMFRETE") });
        JapeWrapper itemNotaDA2 = JapeFactory.dao("AD_SIMFRETE");
        itemNotaDA2.deleteByCriteria("ID = ?", new Object[] { tgfCabVO.asString("AD_IDSIMFRETE") });
      } 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    } finally {
      JapeSession.close(hnd);
    } 
  }
  
  public void beforeCommit(TransactionContext event) throws Exception {}
  
  public void beforeDelete(PersistenceEvent event) throws Exception {}
  
  public void beforeInsert(PersistenceEvent event) throws Exception {
    JapeSession.SessionHandle hnd = null;
    DynamicVO tgfCabVONew = null;
    try {
      hnd = JapeSession.open();
      tgfCabVONew = (DynamicVO)event.getVo();
      if (JapeSession.getProperty("Cabecalho.nunota.duplicando") != null)
        tgfCabVONew.setProperty("AD_IDSIMFRETE", null); 
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    } finally {
      JapeSession.close(hnd);
    } 
  }
}
