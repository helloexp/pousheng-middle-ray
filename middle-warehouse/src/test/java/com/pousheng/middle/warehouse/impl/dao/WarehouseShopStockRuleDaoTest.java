package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18
 */
public class WarehouseShopStockRuleDaoTest extends BaseDaoTest{

    @Autowired
    private WarehouseShopStockRuleDao warehouseShopStockRuleDao;

    private WarehouseShopStockRule rule;


    @Before
    public void setUp() throws Exception {
        rule = make(1L, 3L, 10, 11L);
        warehouseShopStockRuleDao.create(rule);
        assertThat(rule.getId(), notNullValue());
    }

    @Test
    public void findByShopId() throws Exception {
        WarehouseShopStockRule actual = warehouseShopStockRuleDao.findByShopId(rule.getShopId());
        assertThat(actual.getId(), is(rule.getId()));
    }

    private WarehouseShopStockRule make(Long  shopId, Long safeStock, int ratio, Long lastPushStock ) {
        WarehouseShopStockRule r = new WarehouseShopStockRule();
        r.setShopId(shopId);
        r.setShopName("shop"+shopId);
        r.setSafeStock(safeStock);
        r.setRatio(ratio);
        r.setLastPushStock(lastPushStock);
        return r;
    }
}