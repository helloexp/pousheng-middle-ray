package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseShopRule;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
public class WarehouseShopRuleDaoTest extends BaseDaoTest{

    @Autowired
    private WarehouseShopRuleDao warehouseShopRuleDao;

    private WarehouseShopRule warehouseShopRule;

    @Before
    public void setUp() throws Exception {
        warehouseShopRule = make();
        warehouseShopRuleDao.create(warehouseShopRule);
        assertThat(warehouseShopRule.getId(), notNullValue());
    }

    @Test
    public void deleteByRuleId() throws Exception {
        warehouseShopRuleDao.deleteByRuleId(warehouseShopRule.getRuleId());
        List<WarehouseShopRule> byRuleId = warehouseShopRuleDao.findByRuleId(warehouseShopRule.getRuleId());
        assertThat(byRuleId.size(), is(0));
    }

    @Test
    public void findByRuleId() throws Exception {

        List<WarehouseShopRule> byRuleId = warehouseShopRuleDao.findByRuleId(warehouseShopRule.getRuleId());
        assertThat(byRuleId.size(), is(1));
    }

    @Test
    public void findByShopId() throws Exception {
        List<WarehouseShopRule> byShopId = warehouseShopRuleDao.findByShopId(warehouseShopRule.getShopId());
        assertThat(byShopId.size(), is(1));
    }

    private WarehouseShopRule make() {
        WarehouseShopRule warehouseShopRule = new WarehouseShopRule();


        warehouseShopRule.setShopId(1L);
        warehouseShopRule.setShopName("name");

        warehouseShopRule.setRuleId(3L);

        warehouseShopRule.setCreatedAt(new Date());

        warehouseShopRule.setUpdatedAt(new Date());


        return warehouseShopRule;
    }
}