/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: WarehouseSkuStockManagerTest
 * Author:   xiehong
 * Date:     2018/5/30 下午9:30
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.warehouse.impl.service;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.AbstractTest;
import com.pousheng.middle.warehouse.BaseServiceTest;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.impl.dao.WarehouseSkuStockDao;
import com.pousheng.middle.warehouse.manager.WarehouseSkuStockManager;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.*;

/**
 * @author xiehong
 * @create 2018/5/30 下午9:30
 */
public class WarehouseSkuStockManagerTest extends AbstractTest {


    @Configuration
    public static class MockitoBeans {

        @MockBean
        private WarehouseSkuStockDao warehouseSkuStockDao;
        @SpyBean
        private WarehouseSkuStockManager warehouseSkuStockManager;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        warehouseSkuStockDao = get(WarehouseSkuStockDao.class);
        warehouseSkuStockManager = get(WarehouseSkuStockManager.class);

    }

    WarehouseSkuStockDao warehouseSkuStockDao;
    WarehouseSkuStockManager warehouseSkuStockManager;


    @Test
    public void unlockStock() {
        Mockito.when(warehouseSkuStockDao.findByWarehouseIdAndSkuCode(anyLong(),anyString())).thenReturn(new WarehouseSkuStock(){{setLockedStock(123L);}});
        Mockito.when(warehouseSkuStockDao.unlockStock(anyLong(),anyString(),anyInt(),anyLong())).thenReturn(Boolean.TRUE);

        List<WarehouseShipment> warehouseShipments = Lists.newArrayList();
        WarehouseShipment shipment = new WarehouseShipment();
        warehouseShipments.add(shipment);
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList();
        SkuCodeAndQuantity quantity = new SkuCodeAndQuantity(){{setSkuCode("132124");setQuantity(12);}};
        skuCodeAndQuantities.add(quantity);
        shipment.setSkuCodeAndQuantities(skuCodeAndQuantities);

        warehouseSkuStockManager.unlockStock(warehouseShipments);


    }


}