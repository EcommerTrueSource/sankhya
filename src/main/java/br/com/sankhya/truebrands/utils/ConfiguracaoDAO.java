package br.com.sankhya.truebrands.utils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import java.math.BigDecimal;

public class ConfiguracaoDAO {
  public static DynamicVO get() throws Exception {
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    PersistentLocalEntity config = dwfEntityFacade.findEntityByPrimaryKey("AD_INTEGRACAO", new BigDecimal(2));
    EntityVO configEVO = config.getValueObject();
    DynamicVO configVO = (DynamicVO)configEVO;
    return configVO;
  }
}
