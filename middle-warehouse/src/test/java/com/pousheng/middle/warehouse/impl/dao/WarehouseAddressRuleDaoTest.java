package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
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
 * Desc: 地址和仓库规则的关联Dao 测试类
 * Date: 2017-06-07
 */
public class WarehouseAddressRuleDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseAddressRuleDao warehouseAddressRuleDao;

    private WarehouseAddressRule warehouseAddressRule;

    @Before
    public void init() {
        warehouseAddressRule = make();

        warehouseAddressRuleDao.create(warehouseAddressRule);
        assertNotNull(warehouseAddressRule.getId());
    }

    @Test
    public void findById() {
        WarehouseAddressRule warehouseAddressRuleExist = warehouseAddressRuleDao.findById(warehouseAddressRule.getId());

        assertNotNull(warehouseAddressRuleExist);
    }

    @Test
    public void update() {
        warehouseAddressRule.setRuleId(1L);
        warehouseAddressRuleDao.update(warehouseAddressRule);

        WarehouseAddressRule  updated = warehouseAddressRuleDao.findById(warehouseAddressRule.getId());
        assertEquals(updated.getRuleId(), Long.valueOf(1L));
    }

    @Test
    public void delete() {
        warehouseAddressRuleDao.delete(warehouseAddressRule.getId());

        WarehouseAddressRule deleted = warehouseAddressRuleDao.findById(warehouseAddressRule.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ruleId", warehouseAddressRule.getRuleId());
        Paging<WarehouseAddressRule > warehouseAddressRulePaging = warehouseAddressRuleDao.paging(0, 20, params);

        assertThat(warehouseAddressRulePaging.getTotal(), is(1L));
        assertEquals(warehouseAddressRulePaging.getData().get(0).getId(), warehouseAddressRule.getId());
    }

    private WarehouseAddressRule make() {
        WarehouseAddressRule warehouseAddressRule = new WarehouseAddressRule();

        
        warehouseAddressRule.setAddressId(1L);
        warehouseAddressRule.setAddressName("name");

        warehouseAddressRule.setRuleId(3L);
        
        warehouseAddressRule.setCreatedAt(new Date());
        
        warehouseAddressRule.setUpdatedAt(new Date());
        

        return warehouseAddressRule;
    }

}