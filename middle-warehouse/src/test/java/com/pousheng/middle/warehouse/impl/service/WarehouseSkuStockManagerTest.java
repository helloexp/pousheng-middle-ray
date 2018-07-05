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
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.manager.WarehouseSkuStockManager;
import io.terminus.common.model.Response;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.mockito.Matchers.anyList;

/**
 * @author xiehong
 * @create 2018/5/30 下午9:30
 */
public class WarehouseSkuStockManagerTest extends AbstractTest {


    @Configuration
    public static class MockitoBeans {

        @MockBean
        private InventoryClient inventoryClient;
        @SpyBean
        private WarehouseSkuStockManager warehouseSkuStockManager;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        inventoryClient = get(InventoryClient.class);
        warehouseSkuStockManager = get(WarehouseSkuStockManager.class);

    }

    InventoryClient inventoryClient;
    WarehouseSkuStockManager warehouseSkuStockManager;


    @Test
    public void unlockStock() {
        Mockito.when(inventoryClient.unLock(anyList())).thenReturn(Response.ok(true));

        List<WarehouseShipment> warehouseShipments = Lists.newArrayList();
        WarehouseShipment shipment = new WarehouseShipment();
        warehouseShipments.add(shipment);
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList();
        SkuCodeAndQuantity quantity = new SkuCodeAndQuantity(){{setSkuOrderId(11L);setSkuCode("132124");setQuantity(12);}};
        skuCodeAndQuantities.add(quantity);
        shipment.setSkuCodeAndQuantities(skuCodeAndQuantities);

        InventoryTradeDTO inventoryTradeDTO = InventoryTradeDTO.builder()
                .bizSrcId("11").subBizSrcId(Lists.newArrayList("11"))
                .shopId(7L)
                .build();

        warehouseSkuStockManager.unlockStock(inventoryTradeDTO, warehouseShipments);


    }


}