package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
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
 * Author: zhaoxiaowei
 * Desc: Dao 测试类
 * Date: 2018-09-04
 */
public class WarehouseRulePriorityItemDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseRulePriorityItemDao warehouseRulePriorityItemDao;

    private WarehouseRulePriorityItem warehouseRulePriorityItem;

    @Before
    public void init() {
        warehouseRulePriorityItem = make();

        warehouseRulePriorityItemDao.create(warehouseRulePriorityItem);
        assertNotNull(warehouseRulePriorityItem.getId());
    }

    @Test
    public void findById() {
        WarehouseRulePriorityItem warehouseRulePriorityItemExist = warehouseRulePriorityItemDao.findById(warehouseRulePriorityItem.getId());

        assertNotNull(warehouseRulePriorityItemExist);
    }

    @Test
    public void update() {
        // todo
        warehouseRulePriorityItem.setUpdatedAt(new Date());
        warehouseRulePriorityItemDao.update(warehouseRulePriorityItem);

        WarehouseRulePriorityItem  updated = warehouseRulePriorityItemDao.findById(warehouseRulePriorityItem.getId());
        // todo
        //assertEquals(updated.getHasDisplay(), Boolean.TRUE);
    }

    @Test
    public void delete() {
        warehouseRulePriorityItemDao.delete(warehouseRulePriorityItem.getId());

        WarehouseRulePriorityItem deleted = warehouseRulePriorityItemDao.findById(warehouseRulePriorityItem.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        //todo
        //params.put("userId", warehouseRulePriorityItem.getUserId());
        Paging<WarehouseRulePriorityItem > warehouseRulePriorityItemPaging = warehouseRulePriorityItemDao.paging(0, 20, params);

        assertThat(warehouseRulePriorityItemPaging.getTotal(), is(1L));
        assertEquals(warehouseRulePriorityItemPaging.getData().get(0).getId(), warehouseRulePriorityItem.getId());
    }

    public WarehouseRulePriorityItem make() {
        WarehouseRulePriorityItem warehouseRulePriorityItem = new WarehouseRulePriorityItem();
        warehouseRulePriorityItem.setPriorityId(1L);
        warehouseRulePriorityItem.setWarehouseId(1L);
        warehouseRulePriorityItem.setPriority(1);
        return warehouseRulePriorityItem;
    }

}
