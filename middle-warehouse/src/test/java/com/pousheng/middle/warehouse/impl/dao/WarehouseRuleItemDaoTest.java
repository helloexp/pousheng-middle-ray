package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


/**
 * Author: jlchen
 * Desc: 仓库优先级规则项Dao 测试类
 * Date: 2017-06-07
 */
public class WarehouseRuleItemDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseRuleItemDao warehouseRuleItemDao;

    private WarehouseRuleItem warehouseRuleItem;

    @Before
    public void init() {
        warehouseRuleItem = make();

        warehouseRuleItemDao.create(warehouseRuleItem);
        assertNotNull(warehouseRuleItem.getId());
    }

    @Test
    public void findById() {
        WarehouseRuleItem warehouseRuleItemExist = warehouseRuleItemDao.findById(warehouseRuleItem.getId());

        assertNotNull(warehouseRuleItemExist);
    }

    @Test
    public void update() {
        warehouseRuleItem.setRuleId(2L);
        warehouseRuleItemDao.update(warehouseRuleItem);

        WarehouseRuleItem  updated = warehouseRuleItemDao.findById(warehouseRuleItem.getId());
        assertEquals(updated.getRuleId(), Long.valueOf(2));
    }

    @Test
    public void delete() {
        warehouseRuleItemDao.delete(warehouseRuleItem.getId());

        WarehouseRuleItem deleted = warehouseRuleItemDao.findById(warehouseRuleItem.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ruleId", warehouseRuleItem.getRuleId());
        Paging<WarehouseRuleItem > warehouseRuleItemPaging = warehouseRuleItemDao.paging(0, 20, params);

        assertThat(warehouseRuleItemPaging.getTotal(), is(1L));
        assertEquals(warehouseRuleItemPaging.getData().get(0).getId(), warehouseRuleItem.getId());
    }

    private WarehouseRuleItem make() {
        WarehouseRuleItem warehouseRuleItem = new WarehouseRuleItem();

        
        warehouseRuleItem.setRuleId(3L);
        
        warehouseRuleItem.setWarehouseId(2L);
        
        warehouseRuleItem.setName("name");
        
        warehouseRuleItem.setPriority(2);
        
        warehouseRuleItem.setCreatedAt(new Date());
        
        warehouseRuleItem.setUpdatedAt(new Date());
        

        return warehouseRuleItem;
    }

}