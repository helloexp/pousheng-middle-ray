package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import io.terminus.common.model.Paging;
import org.joda.time.DateTime;
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
public class WarehouseRulePriorityDaoTest extends BaseDaoTest {

    @Autowired
    private WarehouseRulePriorityDao warehouseRulePriorityDao;

    private WarehouseRulePriority warehouseRulePriority;

    @Before
    public void init() {
        warehouseRulePriority = make();
        warehouseRulePriorityDao.create(warehouseRulePriority);
        assertNotNull(warehouseRulePriority.getId());
    }

    @Test
    public void findById() {
        WarehouseRulePriority warehouseRulePriorityExist = warehouseRulePriorityDao.findById(warehouseRulePriority.getId());

        assertNotNull(warehouseRulePriorityExist);
    }

    @Test
    public void update() {
        // todo
        warehouseRulePriority.setUpdatedAt(new Date());
        warehouseRulePriorityDao.update(warehouseRulePriority);

        WarehouseRulePriority updated = warehouseRulePriorityDao.findById(warehouseRulePriority.getId());
        // todo
        //assertEquals(updated.getHasDisplay(), Boolean.TRUE);
    }

    @Test
    public void delete() {
        warehouseRulePriorityDao.delete(warehouseRulePriority.getId());

        WarehouseRulePriority deleted = warehouseRulePriorityDao.findById(warehouseRulePriority.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        //todo
        //params.put("userId", warehouseRulePriority.getUserId());
        Paging<WarehouseRulePriority> warehouseRulePriorityPaging = warehouseRulePriorityDao.paging(0, 20, params);

        assertThat(warehouseRulePriorityPaging.getTotal(), is(1L));
        assertEquals(warehouseRulePriorityPaging.getData().get(0).getId(), warehouseRulePriority.getId());
    }

    public WarehouseRulePriority make() {
        WarehouseRulePriority warehouseRulePriority = new WarehouseRulePriority();
        warehouseRulePriority.setRuleId(1L);
        warehouseRulePriority.setName("测试");
        DateTime start = new DateTime(2018, 7, 1, 0, 0, 0);
        DateTime end = new DateTime(2018, 8, 1, 0, 0, 0);
        warehouseRulePriority.setStartDate(start.toDate());
        warehouseRulePriority.setEndDate(end.toDate());
        warehouseRulePriority.setStatus(1);
        return warehouseRulePriority;
    }

    @Test
    public void checkByName() {
        WarehouseRulePriority warehouseRulePriority = new WarehouseRulePriority();
        warehouseRulePriority.setRuleId(1L);
        warehouseRulePriority.setName("测试");
        DateTime start = new DateTime(2018, 7, 1, 0, 0, 0);
        DateTime end = new DateTime(2018, 8, 1, 0, 0, 0);
        warehouseRulePriority.setStartDate(start.toDate());
        warehouseRulePriority.setEndDate(end.toDate());
        warehouseRulePriority.setStatus(1);
        warehouseRulePriorityDao.checkByName(warehouseRulePriority);
        assertThat(warehouseRulePriorityDao.checkTimeRange(warehouseRulePriority), is(false));
        warehouseRulePriority.setRuleId(null);
        assertThat(warehouseRulePriorityDao.checkTimeRange(warehouseRulePriority), is(true));

    }

    @Test
    public void checkTimeRange() {
        WarehouseRulePriority warehouseRulePriority = new WarehouseRulePriority();
        warehouseRulePriority.setRuleId(1L);
        warehouseRulePriority.setName("测试");
        DateTime start = new DateTime(2018, 7, 5, 0, 0, 0);
        DateTime end = new DateTime(2018, 8, 5, 0, 0, 0);
        warehouseRulePriority.setStartDate(start.toDate());
        warehouseRulePriority.setEndDate(end.toDate());
        warehouseRulePriority.setStatus(1);
        assertThat(warehouseRulePriorityDao.checkTimeRange(warehouseRulePriority), is(true));

    }


}
