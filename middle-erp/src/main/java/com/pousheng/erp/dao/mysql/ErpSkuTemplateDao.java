package com.pousheng.erp.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.spu.model.SkuTemplate;
import org.springframework.stereotype.Repository;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-09-14
 */
@Repository
public class ErpSkuTemplateDao extends MyBatisDao<SkuTemplate>{

    /**
     * 逻辑删除对应skuCode的skuTemplate
     *
     * @param skuCode sku编码
     */
    public void logicDeleteBySkuCode(String skuCode){
        getSqlSession().update(sqlId("logicDeleteBySkuCode"), skuCode);
    }
}
