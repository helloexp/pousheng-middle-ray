package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * Author: jlchen
 * Desc: 店铺的退货仓库Dao 测试类
 * Date: 2017-06-21
 */
public class WarehouseCompanyRuleDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseCompanyRuleDao warehouseCompanyRuleDao;

    private WarehouseCompanyRule warehouseCompanyRule;

    @Before
    public void init() {
        warehouseCompanyRule = make();

        warehouseCompanyRuleDao.create(warehouseCompanyRule);
        assertNotNull(warehouseCompanyRule.getId());
    }

    @Test
    public void findById() {
        WarehouseCompanyRule warehouseCompanyRuleExist = warehouseCompanyRuleDao.findById(warehouseCompanyRule.getId());

        assertNotNull(warehouseCompanyRuleExist);
    }

    @Test
    public void update() {
        warehouseCompanyRule.setWarehouseId(44L);
        warehouseCompanyRuleDao.update(warehouseCompanyRule);

        WarehouseCompanyRule updated = warehouseCompanyRuleDao.findById(warehouseCompanyRule.getId());
        assertThat(updated.getWarehouseId(), is(44L));
    }

    @Test
    public void delete() {
        warehouseCompanyRuleDao.delete(warehouseCompanyRule.getId());

        WarehouseCompanyRule deleted = warehouseCompanyRuleDao.findById(warehouseCompanyRule.getId());
        assertNull(deleted);
    }

    @Test
    public void findByCompanyCode() throws Exception {
        WarehouseCompanyRule actual = warehouseCompanyRuleDao.findByCompanyCode(warehouseCompanyRule.getCompanyCode());
        assertThat(actual.getCompanyCode(), is(warehouseCompanyRule.getCompanyCode()));
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("shopId", warehouseCompanyRule.getShopId());
        Paging<WarehouseCompanyRule> warehouseShopReturnPaging = warehouseCompanyRuleDao.paging(0, 20, params);

        assertThat(warehouseShopReturnPaging.getTotal(), is(1L));
        assertEquals(warehouseShopReturnPaging.getData().get(0).getId(), warehouseCompanyRule.getId());
    }

    private WarehouseCompanyRule make() {
        WarehouseCompanyRule warehouseCompanyRule = new WarehouseCompanyRule();

        warehouseCompanyRule.setCompanyCode("001");
        warehouseCompanyRule.setCompanyName("company1");
        warehouseCompanyRule.setShopId(12L);
        warehouseCompanyRule.setShopName("shop12");
        
        warehouseCompanyRule.setWarehouseId(23L);
        
        warehouseCompanyRule.setWarehouseName("NAME");

        return warehouseCompanyRule;
    }

}