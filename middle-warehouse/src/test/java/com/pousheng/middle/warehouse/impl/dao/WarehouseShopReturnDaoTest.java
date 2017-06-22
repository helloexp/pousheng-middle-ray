package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
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
 * Desc: 店铺的退货仓库Dao 测试类
 * Date: 2017-06-21
 */
public class WarehouseShopReturnDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseShopReturnDao warehouseShopReturnDao;

    private WarehouseShopReturn warehouseShopReturn;

    @Before
    public void init() {
        warehouseShopReturn = make();

        warehouseShopReturnDao.create(warehouseShopReturn);
        assertNotNull(warehouseShopReturn.getId());
    }

    @Test
    public void findById() {
        WarehouseShopReturn warehouseShopReturnExist = warehouseShopReturnDao.findById(warehouseShopReturn.getId());

        assertNotNull(warehouseShopReturnExist);
    }

    @Test
    public void update() {
        warehouseShopReturn.setShopId(1L);
        warehouseShopReturnDao.update(warehouseShopReturn);

        WarehouseShopReturn  updated = warehouseShopReturnDao.findById(warehouseShopReturn.getId());
        assertEquals(updated.getShopId(), Long.valueOf(1));
    }

    @Test
    public void delete() {
        warehouseShopReturnDao.delete(warehouseShopReturn.getId());

        WarehouseShopReturn deleted = warehouseShopReturnDao.findById(warehouseShopReturn.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("shopId", warehouseShopReturn.getShopId());
        Paging<WarehouseShopReturn > warehouseShopReturnPaging = warehouseShopReturnDao.paging(0, 20, params);

        assertThat(warehouseShopReturnPaging.getTotal(), is(1L));
        assertEquals(warehouseShopReturnPaging.getData().get(0).getId(), warehouseShopReturn.getId());
    }

    private WarehouseShopReturn make() {
        WarehouseShopReturn warehouseShopReturn = new WarehouseShopReturn();

        
        warehouseShopReturn.setShopId(12L);
        
        warehouseShopReturn.setWarehouseId(23L);
        
        warehouseShopReturn.setWarehouseName("NAME");
        
        warehouseShopReturn.setCreatedAt(new Date());
        
        warehouseShopReturn.setUpdatedAt(new Date());
        

        return warehouseShopReturn;
    }

}