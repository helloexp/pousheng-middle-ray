package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseSku;
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
 * Desc: sku在仓库的库存情况Dao 测试类
 * Date: 2017-06-07
 */
public class WarehouseSkuDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseSkuDao warehouseSkuDao;

    private WarehouseSku warehouseSku;

    @Before
    public void init() {
        warehouseSku = make();

        warehouseSkuDao.create(warehouseSku);
        assertNotNull(warehouseSku.getId());
    }

    @Test
    public void findById() {
        WarehouseSku warehouseSkuExist = warehouseSkuDao.findById(warehouseSku.getId());

        assertNotNull(warehouseSkuExist);
    }

    @Test
    public void update() {
        warehouseSku.setSkuCode("111");
        warehouseSkuDao.update(warehouseSku);

        WarehouseSku  updated = warehouseSkuDao.findById(warehouseSku.getId());
        assertEquals(updated.getSkuCode(), String.valueOf("111"));
    }

    @Test
    public void delete() {
        warehouseSkuDao.delete(warehouseSku.getId());

        WarehouseSku deleted = warehouseSkuDao.findById(warehouseSku.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("skuCode", warehouseSku.getSkuCode());
        Paging<WarehouseSku > warehouseSkuPaging = warehouseSkuDao.paging(0, 20, params);

        assertThat(warehouseSkuPaging.getTotal(), is(1L));
        assertEquals(warehouseSkuPaging.getData().get(0).getId(), warehouseSku.getId());
    }

    private WarehouseSku make() {
        WarehouseSku warehouseSku = new WarehouseSku();

        
        warehouseSku.setWarehouseId(2L);
        
        warehouseSku.setSkuCode("444");
        
        warehouseSku.setBaseStock(12L);
        
        warehouseSku.setAvailStock(32L);
        
        warehouseSku.setLockedStock(4L);
        
        warehouseSku.setSyncAt(new Date());
        
        warehouseSku.setCreatedAt(new Date());
        
        warehouseSku.setUpdatedAt(new Date());
        

        return warehouseSku;
    }

}