package com.pousheng.middle.dispatch;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.open.ReceiverInfoCompleter;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.DispatchOrderEngine;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.web.order.component.DispatchOrderHandler;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.erp.SyncErpShipmentLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.warehouses.algorithm.WarehouseChooser;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @author zhurg
 * @date 2019/5/31 - 下午2:32
 */
public class DispatchTest extends AbstractRestApiTest {

    private SkuOrderReadService skuOrderReadService;

    private ReceiverInfoReadService receiverInfoReadService;

    private DispatchOrderEngine dispatchOrderEngine;

    private ShipmentWiteLogic shipmentWiteLogic;

    private ShipmentReadService shipmentReadService;

    private MiddleOrderWriteService middleOrderWriteService;

    private OrderWriteLogic orderWriteLogic;

    private OrderReadLogic orderReadLogic;

    private OrderWriteService orderWriteService;

    private SyncMposOrderLogic syncMposOrderLogic;

    private ReceiverInfoCompleter receiverInfoCompleter;

    private WarehouseChooser warehouseChooser;

    private WarehouseCacher warehouseCacher;

    private MiddleShopCacher middleShopCacher;

    private SyncErpShipmentLogic syncErpShipmentLogic;

    @InjectMocks
    private DispatchOrderHandler dispatchOrderHandler;

    @Configuration
    public static class MockitoBeans {

        @MockBean
        private SkuOrderReadService skuOrderReadService;

        @MockBean
        private ReceiverInfoReadService receiverInfoReadService;

        @MockBean
        private DispatchOrderEngine dispatchOrderEngine;

        @MockBean
        private ShipmentWiteLogic shipmentWiteLogic;

        @MockBean
        private ShipmentReadService shipmentReadService;

        @MockBean
        private MiddleOrderWriteService middleOrderWriteService;

        @MockBean
        private OrderWriteLogic orderWriteLogic;

        @MockBean
        private OrderReadLogic orderReadLogic;

        @MockBean
        private OrderWriteService orderWriteService;

        @MockBean
        private SyncMposOrderLogic syncMposOrderLogic;

        @MockBean
        private ReceiverInfoCompleter receiverInfoCompleter;

        @MockBean
        private WarehouseChooser warehouseChooser;

        @MockBean
        private WarehouseCacher warehouseCacher;

        @MockBean
        private MiddleShopCacher middleShopCacher;

        @MockBean
        private SyncErpShipmentLogic syncErpShipmentLogic;


    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        //dispatchOrderHandler = get(DispatchOrderHandler.class);
        dispatchOrderEngine = get(DispatchOrderEngine.class);
        skuOrderReadService = get(SkuOrderReadService.class);
        receiverInfoReadService = get(ReceiverInfoReadService.class);
        shipmentWiteLogic = get(ShipmentWiteLogic.class);
        shipmentReadService = get(ShipmentReadService.class);
        middleOrderWriteService = get(MiddleOrderWriteService.class);
        orderWriteLogic = get(OrderWriteLogic.class);
        orderReadLogic = get(OrderReadLogic.class);
        orderWriteService = get(OrderWriteService.class);
        syncMposOrderLogic = get(SyncMposOrderLogic.class);
        receiverInfoCompleter = get(ReceiverInfoCompleter.class);
        warehouseChooser = get(WarehouseChooser.class);
        warehouseCacher = get(WarehouseCacher.class);
        middleShopCacher = get(MiddleShopCacher.class);
        syncErpShipmentLogic = get(SyncErpShipmentLogic.class);
    }

    @Before
    public void initMocks() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(dispatchOrderHandler, "dispatchRetryTimes", 5);
    }

    @Test
    public void testToDispatch() throws Exception {

        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setId(1L);
        shopOrder.setOrderCode("aaaaa");
        shopOrder.setShopId(1L);
        Map<String, String> extraJson = Maps.newHashMap();
        //MPOS
        extraJson.put(TradeConstants.IS_HK_POS_ORDER, "Y");
        shopOrder.setExtraJson(JSON.toJSONString(extraJson));

        List<SkuOrder> skuOrders = Lists.newArrayList();
        SkuOrder order1 = new SkuOrder();
        order1.setId(1L);
        order1.setSkuCode("001");
        order1.setWithHold(2);
        order1.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
        skuOrders.add(order1);

        SkuOrder order2 = new SkuOrder();
        order2.setId(2L);
        order2.setSkuCode("002");
        order2.setWithHold(1);
        order2.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
        skuOrders.add(order2);

        Response<List<SkuOrder>> skuOrdersResponse = new Response<>();
        skuOrdersResponse.setSuccess(true);
        skuOrdersResponse.setResult(skuOrders);

        when(skuOrderReadService.findByShopOrderId(anyLong())).thenReturn(skuOrdersResponse);

        Response<List<ReceiverInfo>> reveiverResponse = new Response<>();
        reveiverResponse.setSuccess(true);
        reveiverResponse.setResult(Lists.newArrayList(new ReceiverInfo()));
        when(receiverInfoReadService.findByOrderId(1L, OrderLevel.SHOP)).thenReturn(reveiverResponse);

        SkuCodeAndQuantity sku1 = new SkuCodeAndQuantity();
        sku1.setSkuOrderId(1L);
        sku1.setSkuCode("001");
        sku1.setQuantity(2);
        sku1.setShipQuantity(2);

        SkuCodeAndQuantity sku2 = new SkuCodeAndQuantity();
        sku2.setSkuOrderId(2L);
        sku2.setSkuCode("002");
        sku2.setQuantity(1);
        sku2.setShipQuantity(1);

        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList(
                sku1, sku2
        );

        DispatchOrderItemInfo dispatchOrderItemInfo = new DispatchOrderItemInfo();
        //能发货的
        WarehouseShipment warehouseShipment1 = new WarehouseShipment();

        //整单
        warehouseShipment1.setSkuCodeAndQuantities(skuCodeAndQuantities);
        warehouseShipment1.setWarehouseId(1L);
        warehouseShipment1.setWarehouseName("测试仓库");
        dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList(warehouseShipment1));

        //部分发货
        //warehouseShipment1.setSkuCodeAndQuantities(Lists.newArrayList(sku1));
        //warehouseShipment1.setWarehouseId(1L);
        //warehouseShipment1.setWarehouseName("测试仓库");
        //dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList(warehouseShipment1));
        //dispatchOrderItemInfo.setSkuCodeAndQuantities(Lists.newArrayList(sku2));


        Response<DispatchOrderItemInfo> dispatchOrderItemInfoResponse = new Response<>();
        dispatchOrderItemInfoResponse.setSuccess(true);
        dispatchOrderItemInfoResponse.setResult(dispatchOrderItemInfo);

        //正常寻源流程
        when(dispatchOrderEngine.toDispatchOrder(any(ShopOrder.class), any(ReceiverInfo.class), anyListOf(SkuCodeAndQuantity.class))).thenReturn(dispatchOrderItemInfoResponse);

        //正常流程
        when(shipmentWiteLogic.createShipment(any(ShopOrder.class), anyList(), any(WarehouseShipment.class))).thenReturn(new Random().nextLong());

        //占库失败
        //when(shipmentWiteLogic.createShipment(any(ShopOrder.class), anyList(), any(WarehouseShipment.class))).thenThrow(new RuntimeException("占库失败"));

        //全渠道
        when(orderReadLogic.isAllChannelOpenShop(1L)).thenReturn(true);

        when(orderReadLogic.findSkuOrdersByIds(anyListOf(Long.class))).thenReturn(skuOrders);

        Response<Shipment> shipmentResponse = new Response<>();
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setExtra(Maps.newHashMap());
        shipment.setSkuInfos(ImmutableMap.of(1L, 2, 2L, 1));
        shipmentResponse.setResult(shipment);
        shipmentResponse.setSuccess(true);
        when(shipmentReadService.findById(anyLong())).thenReturn(shipmentResponse);
        Response<Boolean> booleanResponse = new Response<>();
        booleanResponse.setSuccess(true);
        when(middleOrderWriteService.updateShopOrder(any(ShopOrder.class))).thenReturn(booleanResponse);

        doNothing().when(shipmentWiteLogic).updateShipmentNote(any(ShopOrder.class), anyInt());
        doNothing().when(orderWriteLogic).updateSkuHandleNumber(anyMapOf(Long.class, Integer.class));
        doNothing().when(shipmentWiteLogic).handleSyncShipment(any(Shipment.class), anyInt(), any(ShopOrder.class));

        dispatchOrderHandler.toDispatchOrderNew(shopOrder, Collections.emptyList());
    }

    @Test
    public void testAutoHandleAllChannelOrderByNewLogic() throws Exception {
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setId(1L);
        shopOrder.setOrderCode("aaaaa");
        shopOrder.setShopId(1L);
        Map<String, String> extraJson = Maps.newHashMap();
        //MPOS
        extraJson.put(TradeConstants.IS_HK_POS_ORDER, "Y");
        shopOrder.setExtraJson(JSON.toJSONString(extraJson));

        List<SkuOrder> skuOrders = Lists.newArrayList();
        SkuOrder order1 = new SkuOrder();
        order1.setId(1L);
        order1.setSkuCode("001");
        order1.setWithHold(1);
        order1.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
        skuOrders.add(order1);

        SkuOrder order2 = new SkuOrder();
        order2.setId(2L);
        order2.setSkuCode("002");
        order2.setWithHold(1);
        order2.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
        skuOrders.add(order2);

        Response<List<SkuOrder>> skuOrdersResponse = new Response<>();
        skuOrdersResponse.setSuccess(true);
        skuOrdersResponse.setResult(skuOrders);

        when(skuOrderReadService.findByShopOrderId(anyLong())).thenReturn(skuOrdersResponse);

        Response<List<ReceiverInfo>> reveiverResponse = new Response<>();
        reveiverResponse.setSuccess(true);
        ReceiverInfo receiverInfo = new ReceiverInfo();
        receiverInfo.setCity("青岛");
        receiverInfo.setCityId(1);
        reveiverResponse.setResult(Lists.newArrayList(receiverInfo));

        when(receiverInfoReadService.findByOrderId(1L, OrderLevel.SHOP)).thenReturn(reveiverResponse);

        SkuCodeAndQuantity sku1 = new SkuCodeAndQuantity();
        sku1.setSkuOrderId(1L);
        sku1.setSkuCode("001");
        sku1.setQuantity(1);
        sku1.setShipQuantity(1);

        SkuCodeAndQuantity sku2 = new SkuCodeAndQuantity();
        sku2.setSkuOrderId(2L);
        sku2.setSkuCode("002");
        sku2.setQuantity(1);
        sku2.setShipQuantity(1);

        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList(
                sku1, sku2
        );

        when(orderReadLogic.getSkuExtraMapValueByKey(anyString(), any(SkuOrder.class))).thenReturn("1");

        //整单发货
        WarehouseShipment warehouseShipment1 = new WarehouseShipment();
        warehouseShipment1.setWarehouseId(1L);
        warehouseShipment1.setWarehouseName("xxxx");
        warehouseShipment1.setSkuCodeAndQuantities(skuCodeAndQuantities);
        List<WarehouseShipment> warehouseShipments = Lists.newArrayList();
        warehouseShipments.add(warehouseShipment1);

        doNothing().when(receiverInfoCompleter).complete(any(ReceiverInfo.class));

        //正常寻源流程
        when(warehouseChooser.chooseByRegion(any(ReceiverInfo.class), any(ShopOrder.class), anyLong(), anyListOf(SkuCodeAndQuantity.class))).thenReturn(warehouseShipments);

        //仓发
        WarehouseDTO warehouseDTO = new WarehouseDTO();
        warehouseDTO.setWarehouseSubType(0);
        warehouseDTO.setOutCode("xxxx");
        warehouseDTO.setCompanyId("111");
        when(warehouseCacher.findById(anyLong())).thenReturn(warehouseDTO);

        //正常流程
        //when(shipmentWiteLogic.createShipment(any(ShopOrder.class), anyList(), any(WarehouseShipment.class))).thenReturn(new Random().nextLong());

        //整单占库失败
        when(shipmentWiteLogic.createShipment(any(ShopOrder.class), anyList(), any(WarehouseShipment.class))).thenThrow(new RuntimeException("占库失败"));

        when(orderReadLogic.findSkuOrdersByIds(anyListOf(Long.class))).thenReturn(skuOrders);

        Response<Shipment> shipmentResponse = new Response<>();
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setExtra(Maps.newHashMap());
        shipment.setSkuInfos(ImmutableMap.of(1L, 2, 2L, 1));
        shipmentResponse.setResult(shipment);
        shipmentResponse.setSuccess(true);
        when(shipmentReadService.findById(anyLong())).thenReturn(shipmentResponse);
        Response<Boolean> booleanResponse = new Response<>();
        booleanResponse.setSuccess(true);
        when(middleOrderWriteService.updateShopOrder(any(ShopOrder.class))).thenReturn(booleanResponse);

        doNothing().when(shipmentWiteLogic).updateShipmentNote(any(ShopOrder.class), anyInt());
        doNothing().when(orderWriteLogic).updateSkuHandleNumber(anyMapOf(Long.class, Integer.class));
        doNothing().when(shipmentWiteLogic).handleSyncShipment(any(Shipment.class), anyInt(), any(ShopOrder.class));
        when(orderReadLogic.findSkuOrderByShopOrderIdAndStatus(anyLong(), anyInt())).thenReturn(skuOrders);

        //全渠道新派单逻辑
        dispatchOrderHandler.autoCreateAllChannelShipment(shopOrder, skuOrders, true);
    }

    @Test
    public void testAutoCreateShipmentLogic() throws Exception {
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setId(1L);
        shopOrder.setOrderCode("aaaaa");
        shopOrder.setShopId(1L);
        Map<String, String> extraJson = Maps.newHashMap();
        //MPOS
        extraJson.put(TradeConstants.IS_HK_POS_ORDER, "Y");
        shopOrder.setExtraJson(JSON.toJSONString(extraJson));

        List<SkuOrder> skuOrders = Lists.newArrayList();
        SkuOrder order1 = new SkuOrder();
        order1.setId(1L);
        order1.setSkuCode("001");
        order1.setWithHold(1);
        order1.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
        skuOrders.add(order1);

        SkuOrder order2 = new SkuOrder();
        order2.setId(2L);
        order2.setSkuCode("002");
        order2.setWithHold(1);
        order2.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
        skuOrders.add(order2);

        Response<List<SkuOrder>> skuOrdersResponse = new Response<>();
        skuOrdersResponse.setSuccess(true);
        skuOrdersResponse.setResult(skuOrders);

        when(skuOrderReadService.findByShopOrderId(anyLong())).thenReturn(skuOrdersResponse);

        Response<List<ReceiverInfo>> reveiverResponse = new Response<>();
        reveiverResponse.setSuccess(true);
        ReceiverInfo receiverInfo = new ReceiverInfo();
        receiverInfo.setCity("青岛");
        receiverInfo.setCityId(1);
        reveiverResponse.setResult(Lists.newArrayList(receiverInfo));

        when(receiverInfoReadService.findByOrderId(1L, OrderLevel.SHOP)).thenReturn(reveiverResponse);

        SkuCodeAndQuantity sku1 = new SkuCodeAndQuantity();
        sku1.setSkuOrderId(1L);
        sku1.setSkuCode("001");
        sku1.setQuantity(1);
        sku1.setShipQuantity(1);

        SkuCodeAndQuantity sku2 = new SkuCodeAndQuantity();
        sku2.setSkuOrderId(2L);
        sku2.setSkuCode("002");
        sku2.setQuantity(1);
        sku2.setShipQuantity(1);

        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList(
                sku1, sku2
        );

        when(orderReadLogic.getSkuExtraMapValueByKey(anyString(), any(SkuOrder.class))).thenReturn("1");

        //整单发货
        WarehouseShipment warehouseShipment1 = new WarehouseShipment();
        warehouseShipment1.setWarehouseId(1L);
        warehouseShipment1.setWarehouseName("xxxx");
        warehouseShipment1.setSkuCodeAndQuantities(skuCodeAndQuantities);
        List<WarehouseShipment> warehouseShipments = Lists.newArrayList();
        warehouseShipments.add(warehouseShipment1);

        doNothing().when(receiverInfoCompleter).complete(any(ReceiverInfo.class));

        //正常寻源流程
        when(warehouseChooser.choose(any(ShopOrder.class), anyLong(), anyListOf(SkuCodeAndQuantity.class))).thenReturn(warehouseShipments);

        //仓发
        WarehouseDTO warehouseDTO = new WarehouseDTO();
        warehouseDTO.setWarehouseSubType(0);
        warehouseDTO.setOutCode("xxxx");
        warehouseDTO.setCompanyId("111");
        when(warehouseCacher.findById(anyLong())).thenReturn(warehouseDTO);

        //正常流程
        //when(shipmentWiteLogic.createShipment(any(ShopOrder.class), anyList(), any(WarehouseShipment.class))).thenReturn(new Random().nextLong());

        //整单占库失败
        when(shipmentWiteLogic.createShipment(any(ShopOrder.class), anyList(), any(WarehouseShipment.class))).thenThrow(new RuntimeException("占库失败"));

        when(orderReadLogic.findSkuOrdersByIds(anyListOf(Long.class))).thenReturn(skuOrders);

        Response<Boolean> response = new Response<Boolean>();
        response.setSuccess(true);
        when(syncErpShipmentLogic.syncShipment(any(Shipment.class))).thenReturn(response);

        Response<Shipment> shipmentResponse = new Response<>();
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setExtra(Maps.newHashMap());
        shipment.setSkuInfos(ImmutableMap.of(1L, 2, 2L, 1));
        shipmentResponse.setResult(shipment);
        shipmentResponse.setSuccess(true);
        when(shipmentReadService.findById(anyLong())).thenReturn(shipmentResponse);
        Response<Boolean> booleanResponse = new Response<>();
        booleanResponse.setSuccess(true);
        when(middleOrderWriteService.updateShopOrder(any(ShopOrder.class))).thenReturn(booleanResponse);

        doNothing().when(shipmentWiteLogic).updateShipmentNote(any(ShopOrder.class), anyInt());
        doNothing().when(orderWriteLogic).updateSkuHandleNumber(anyMapOf(Long.class, Integer.class));
        doNothing().when(shipmentWiteLogic).handleSyncShipment(any(Shipment.class), anyInt(), any(ShopOrder.class));
        when(orderReadLogic.findSkuOrderByShopOrderIdAndStatus(anyLong(), anyInt())).thenReturn(skuOrders);

        //全渠道新派单逻辑
        dispatchOrderHandler.autoCreateAllChannelShipment(shopOrder, skuOrders, false);
    }
}