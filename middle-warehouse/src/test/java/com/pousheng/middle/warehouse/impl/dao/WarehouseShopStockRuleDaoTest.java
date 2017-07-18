package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

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

    @Test
    public void pagination() throws Exception {
        WarehouseShopStockRule rule2 = make(2L, 3L, 20, 12L);
        warehouseShopStockRuleDao.create(rule2);

        WarehouseShopStockRule rule3 = make(3L, 4L, 30, 14L);
        warehouseShopStockRuleDao.create(rule3);

        Map<String, Object> params = Maps.newHashMap();
        params.put("shopIds", Lists.newArrayList(1L, 3L));
        Paging<WarehouseShopStockRule> p = warehouseShopStockRuleDao.paging(0, 10, params);
        assertThat(p.getTotal(), is(2L));
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