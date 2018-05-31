package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况Dao 测试类
 * Date: 2017-06-07
 */
public class WarehouseSkuStockDaoTest extends BaseDaoTest {



    @Autowired
    private WarehouseSkuStockDao warehouseSkuStockDao;

    private WarehouseSkuStock warehouseSkuStock;

    @Before
    public void init() {
        warehouseSkuStock = make("same");

        warehouseSkuStockDao.create(warehouseSkuStock);
        assertNotNull(warehouseSkuStock.getId());
    }

    @Test
    public void findById() {
        WarehouseSkuStock warehouseSkuStockExist = warehouseSkuStockDao.findById(warehouseSkuStock.getId());

        assertNotNull(warehouseSkuStockExist);
    }

    @Test
    public void update() {
        warehouseSkuStock.setLockedStock(333L);
        warehouseSkuStockDao.update(warehouseSkuStock);

        WarehouseSkuStock updated = warehouseSkuStockDao.findById(warehouseSkuStock.getId());
        assertThat(updated.getLockedStock(), is(333L));
    }

    @Test
    public void delete() {
        warehouseSkuStockDao.delete(warehouseSkuStock.getId());

        WarehouseSkuStock deleted = warehouseSkuStockDao.findById(warehouseSkuStock.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("skuCode", warehouseSkuStock.getSkuCode());
        Paging<WarehouseSkuStock> warehouseSkuPaging = warehouseSkuStockDao.paging(0, 20, params);

        assertThat(warehouseSkuPaging.getTotal(), is(1L));
        assertEquals(warehouseSkuPaging.getData().get(0).getId(), warehouseSkuStock.getId());
    }

    @Test
    public void pagingByDistinctSkuCode() throws Exception {
        for(int i=0; i<20; i++){
            String skuCode = i%2==0? "same": "other";
            WarehouseSkuStock wss = make(skuCode);
            warehouseSkuStockDao.create(wss);
        }

        Paging<WarehouseSkuStock> p = warehouseSkuStockDao.pagingByDistinctSkuCode(0,10,
                Maps.<String, Object>newHashMap());
        assertThat(p.getTotal(), is(2L));
    }

    @Test
    public void decreaseStock() throws Exception {
        boolean success = warehouseSkuStockDao.decreaseStock(warehouseSkuStock.getWarehouseId(), warehouseSkuStock.getSkuCode(),
                warehouseSkuStock.getAvailStock().intValue()-1);
        assertThat(success, is(true));
        WarehouseSkuStock actual = warehouseSkuStockDao.findById(warehouseSkuStock.getId());
        assertThat(actual.getAvailStock(), is(1L));
    }

    @Test
    public void insufficientStock() throws Exception {
        boolean fail = warehouseSkuStockDao.decreaseStock(warehouseSkuStock.getWarehouseId(), warehouseSkuStock.getSkuCode(),
                warehouseSkuStock.getAvailStock().intValue()+1);
        assertThat(fail, is(false));
        WarehouseSkuStock actual = warehouseSkuStockDao.findById(warehouseSkuStock.getId());
        assertThat(actual.getAvailStock(), is(warehouseSkuStock.getAvailStock()));
    }

    @Test
    public void syncStock() throws Exception {

        warehouseSkuStockDao.syncStock(warehouseSkuStock.getWarehouseId(), warehouseSkuStock.getSkuCode(), 2, new Date());

        WarehouseSkuStock actual = warehouseSkuStockDao.findById(warehouseSkuStock.getId());
        assertThat(actual.getBaseStock(), is(2L));
        assertThat(actual.getAvailStock(), is(2-warehouseSkuStock.getLockedStock()));
        assertThat(actual.getLockedStock(), is(warehouseSkuStock.getLockedStock()));

    }

    @Test
    public void syncOutdatedStock() throws Exception {
        warehouseSkuStockDao.syncStock(warehouseSkuStock.getWarehouseId(), warehouseSkuStock.getSkuCode(), 2,
                Date.from(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        WarehouseSkuStock actual = warehouseSkuStockDao.findById(warehouseSkuStock.getId());
        assertThat(actual.getBaseStock(), is(warehouseSkuStock.getBaseStock()));
        assertThat(actual.getAvailStock(), is(warehouseSkuStock.getAvailStock()));
        assertThat(actual.getLockedStock(), is(warehouseSkuStock.getLockedStock()));
    }

    private WarehouseSkuStock make(String skuCode) {
        WarehouseSkuStock warehouseSkuStock = new WarehouseSkuStock();


        warehouseSkuStock.setWarehouseId(2L);

        warehouseSkuStock.setSkuCode(skuCode);

        warehouseSkuStock.setBaseStock(12L);

        warehouseSkuStock.setAvailStock(32L);

        warehouseSkuStock.setLockedStock(4L);

        warehouseSkuStock.setSyncAt(Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        warehouseSkuStock.setCreatedAt(new Date());

        warehouseSkuStock.setUpdatedAt(new Date());
        return warehouseSkuStock;
    }

}