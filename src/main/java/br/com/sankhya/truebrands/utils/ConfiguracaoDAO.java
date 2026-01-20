package br.com.sankhya.truebrands.utils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import java.math.BigDecimal;

public class ConfiguracaoDAO {
  public static DynamicVO get() throws Exception {
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    JapeWrapper dao = JapeFactory.dao("AD_CFGINT");

      return dao.findByPK(new BigDecimal(2));
  }
}
