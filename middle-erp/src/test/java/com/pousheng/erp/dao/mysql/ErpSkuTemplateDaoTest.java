package com.pousheng.erp.dao.mysql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pousheng.erp.dao.BaseDaoTest;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.spu.model.SkuTemplate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-09-14
 */
public class ErpSkuTemplateDaoTest extends BaseDaoTest {

    @Autowired
    private ErpSkuTemplateDao erpSkuTemplateDao;

    @Test
    public void logicDeleteBySkuCode() throws Exception {
        SkuTemplate st = make(1);
        erpSkuTemplateDao.create(st);
        assertThat(erpSkuTemplateDao.findById(st.getId()), notNullValue());
        erpSkuTemplateDao.logicDeleteBySkuCode(st.getSkuCode());
        SkuTemplate actual = erpSkuTemplateDao.findById(st.getId());
        assertThat(actual.getStatus(),is(-3));
    }

    private SkuTemplate make(Integer i) {
        SkuTemplate skuTemplate = new SkuTemplate();
        skuTemplate.setName("sku-template"+new Random().nextInt());
        skuTemplate.setSkuCode("skuCode"+i);
        skuTemplate.setSpuId(1L);
        skuTemplate.setStatus(1);
        skuTemplate.setStockType(1);
        skuTemplate.setPrice(100);
        skuTemplate.setExtraPrice(ImmutableMap.of("originPrice", 300));
        skuTemplate.setImage("image"+i);
        SkuAttribute skuAttribute = new SkuAttribute();
        skuAttribute.setAttrKey("颜色");
        skuAttribute.setAttrVal("红色");
        skuTemplate.setAttrs(ImmutableList.of(skuAttribute));
        skuTemplate.setStockQuantity(20);
        skuTemplate.setSpecification("sku-spec"+i);
        skuTemplate.setExtraJson("{\"key\":\"value\"}");
        return skuTemplate;
    }

}