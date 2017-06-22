package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.Warehouse;
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
 * Desc: 仓库Dao 测试类
 * Date: 2017-06-07
 */
public class WarehouseDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseDao warehouseDao;

    private Warehouse warehouse;

    @Before
    public void init() throws Exception{
        warehouse = make();

        warehouseDao.create(warehouse);
        assertNotNull(warehouse.getId());
    }

    @Test
    public void findById() {
        Warehouse warehouseExist = warehouseDao.findById(warehouse.getId());

        assertNotNull(warehouseExist);
    }

    @Test
    public void update() {
        warehouse.setCode("123");
        warehouseDao.update(warehouse);

        Warehouse  updated = warehouseDao.findById(warehouse.getId());
        assertEquals(updated.getCode(), String.valueOf("123"));
    }

    @Test
    public void delete() {
        warehouseDao.delete(warehouse.getId());

        Warehouse deleted = warehouseDao.findById(warehouse.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("code", warehouse.getCode());
        Paging<Warehouse > warehousePaging = warehouseDao.paging(0, 20, params);

        assertThat(warehousePaging.getTotal(), is(1L));
        assertEquals(warehousePaging.getData().get(0).getId(), warehouse.getId());
    }

    private Warehouse make() throws Exception{
        Warehouse warehouse = new Warehouse();

        
        warehouse.setCode("233333");
        
        warehouse.setName("name");
        
        warehouse.setOwnerId(2l);
        
        warehouse.setIsDefault(Boolean.FALSE);
        
        warehouse.setExtra(ImmutableMap.of("key","json"));
        
        warehouse.setCreatedAt(new Date());
        
        warehouse.setUpdatedAt(new Date());
        

        return warehouse;
    }

}