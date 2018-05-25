package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.SkuStockTask;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


/**
 * Author: songrenfei
 * Desc: sku库存同步任务Dao 测试类
 * Date: 2018-05-24
 */
public class SkuStockTaskDaoTest extends BaseDaoTest {



    @Autowired
    private SkuStockTaskDao skuStockTaskDao;

    private SkuStockTask skuStockTask;

    @Before
    public void init() {
        skuStockTask = make();

        skuStockTaskDao.create(skuStockTask);
        assertNotNull(skuStockTask.getId());
    }

    @Test
    public void findById() {
        SkuStockTask skuStockTaskExist = skuStockTaskDao.findById(skuStockTask.getId());

        assertNotNull(skuStockTaskExist);
    }

    @Test
    public void update() {
        skuStockTask.setStatus(1);
        skuStockTaskDao.update(skuStockTask);

        SkuStockTask  updated = skuStockTaskDao.findById(skuStockTask.getId());
        assertEquals(updated.getStatus(), Integer.valueOf(1));
    }

    @Test
    public void delete() {
        skuStockTaskDao.delete(skuStockTask.getId());

        SkuStockTask deleted = skuStockTaskDao.findById(skuStockTask.getId());
        assertNull(deleted);
    }



    @Test
    public void updateToHandle() {
        Boolean toHandle = skuStockTaskDao.updateToHandle(skuStockTask.getId(),new Date());
        assertEquals(toHandle,Boolean.TRUE);
    }


    @Test
    public void findWaiteHandleLimit() {
        List<SkuStockTask> skuStockTaskList = skuStockTaskDao.findWaiteHandleLimit();
        assertNotNull(skuStockTaskList);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", skuStockTask.getStatus());
        Paging<SkuStockTask > skuStockTaskPaging = skuStockTaskDao.paging(0, 20, params);

        assertThat(skuStockTaskPaging.getTotal(), is(1L));
        assertEquals(skuStockTaskPaging.getData().get(0).getId(), skuStockTask.getId());
    }

    private SkuStockTask make() {
        SkuStockTask skuStockTask = new SkuStockTask();

        
        skuStockTask.setStatus(0);
        
        skuStockTask.setSkuCount(300);

        try {
            skuStockTask.setSkuJson("json");
        } catch (Exception e) {
            e.printStackTrace();
        }

        skuStockTask.setTimeoutAt(new Date());
        
        skuStockTask.setCreatedAt(new Date());
        
        skuStockTask.setUpdatedAt(new Date());
        

        return skuStockTask;
    }

}