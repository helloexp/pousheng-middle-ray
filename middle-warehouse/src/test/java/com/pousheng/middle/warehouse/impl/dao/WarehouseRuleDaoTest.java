package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseRule;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述Dao 测试类
 * Date: 2017-06-07
 */
public class WarehouseRuleDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseRuleDao warehouseRuleDao;

    private WarehouseRule warehouseRule;

    @Before
    public void init() {
        warehouseRule = make();

        warehouseRuleDao.create(warehouseRule);
        assertNotNull(warehouseRule.getId());
    }

    @Test
    public void findById() {
        WarehouseRule warehouseRuleExist = warehouseRuleDao.findById(warehouseRule.getId());

        assertNotNull(warehouseRuleExist);
    }

    @Test
    public void update() {
        warehouseRule.setName("1");
        warehouseRuleDao.update(warehouseRule);

        WarehouseRule  updated = warehouseRuleDao.findById(warehouseRule.getId());
        assertEquals(updated.getName(), String.valueOf("1"));
    }

    @Test
    public void delete() {
        warehouseRuleDao.delete(warehouseRule.getId());

        WarehouseRule deleted = warehouseRuleDao.findById(warehouseRule.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", warehouseRule.getName());
        Paging<WarehouseRule > warehouseRulePaging = warehouseRuleDao.paging(0, 20, params);

        assertThat(warehouseRulePaging.getTotal(), is(1L));
        assertEquals(warehouseRulePaging.getData().get(0).getId(), warehouseRule.getId());
    }

    private WarehouseRule make() {
        WarehouseRule warehouseRule = new WarehouseRule();

        
        warehouseRule.setName("12");
        
        warehouseRule.setCreatedAt(new Date());
        
        warehouseRule.setUpdatedAt(new Date());
        

        return warehouseRule;
    }

}