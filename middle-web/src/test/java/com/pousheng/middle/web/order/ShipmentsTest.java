/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: ShipmentsTest
 * Author:   xiehong
 * Date:     2018/5/29 下午4:56
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.web.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.service.MiddleShipmentWriteService;
import com.pousheng.middle.order.service.OrderShipmentWriteService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.erp.SyncErpShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import com.pousheng.middle.web.warehouses.component.WarehouseSkuStockLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

/**
 *
 * @author xiehong
 * @create 2018/5/29 下午4:56
 */
public class ShipmentsTest extends AbstractRestApiTest {



    @Configuration
    public static class MockitoBeans {

        @MockBean
        private ObjectMapper objectMapper;
        @MockBean
        private OrderReadLogic orderReadLogic;
        @MockBean
        private OrderWriteLogic orderWriteLogic;
        @MockBean
        private WarehouseReadService warehouseReadService;
        @MockBean
        private MiddleOrderFlowPicker orderFlowPicker;
        @MockBean
        private MiddleShipmentWriteService middleShipmentWriteService;
        @MockBean
        private EventBus eventBus;
        @MockBean
        private ShipmentReadLogic shipmentReadLogic;
        @MockBean
        private RefundReadLogic refundReadLogic;
        @MockBean
        private ShipmentWiteLogic shipmentWiteLogic;
        @MockBean
        private SyncErpShipmentLogic syncErpShipmentLogic;
        @MockBean
        private WarehouseSkuWriteService warehouseSkuWriteService;
        @MockBean
        private StockPusher stockPusher;
        @MockBean
        private PermissionUtil permissionUtil;
        @MockBean
        private PoushengSettlementPosReadService poushengSettlementPosReadService;
        @MockBean
        private PoushengSettlementPosWriteService poushengSettlementPosWriteService;
        @RpcConsumer
        private RefundReadService refundReadService;
        @MockBean
        private SyncMposShipmentLogic syncMposShipmentLogic;
        @MockBean
        private SyncShipmentPosLogic syncShipmentPosLogic;
        @MockBean
        private WarehouseSkuStockLogic warehouseSkuStockLogic;
        @MockBean
        private MposSkuStockLogic mposSkuStockLogic;
        @MockBean
        private MiddleShopCacher middleShopCacher;
        @MockBean
        private ShopReadService shopReadService;
        @MockBean
        private SkuTemplateReadService skuTemplateReadService;
        @MockBean
        private SkuOrderReadService skuOrderReadService;
        @MockBean
        private OrderShipmentWriteService orderShipmentWriteService;
        @MockBean
        private ShipmentWriteManger shipmentWriteManger;

        @MockBean
        private ShopCacher shopCacher;

        @SpyBean
        private Shipments shipments;
    }


    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    ShipmentReadLogic shipmentReadLogic;
    ShipmentWiteLogic shipmentWiteLogic;
    MposSkuStockLogic mposSkuStockLogic;
    Shipments shipments;

    @Override
    protected void init() {
        shipmentReadLogic = get(ShipmentReadLogic.class);
        shipmentWiteLogic = get(ShipmentWiteLogic.class);
        mposSkuStockLogic = get(MposSkuStockLogic.class);
        shipments = get(Shipments.class);
    }


    @Test
    public void updateReceiverInfoAndBuyerNote() {
        when(shipmentReadLogic.findShipmentById(anyLong())).thenReturn(new Shipment());
        when(shipmentWiteLogic.updateStatus(any(),any())).thenReturn(Response.ok());
        when(shipmentReadLogic.getShipmentExtra(any())).thenReturn(new ShipmentExtra());
        when(mposSkuStockLogic.unLockStock(any())).thenReturn(Response.ok());
        shipments.cancleShipment(anyLong());
    }
}