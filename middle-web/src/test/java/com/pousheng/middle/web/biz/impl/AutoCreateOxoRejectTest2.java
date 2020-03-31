package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.order.dto.SubmitRefundInfo;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.web.order.component.*;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ShipmentWriteService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author zhurg
 * @date 2019/6/19 - 上午11:17
 */
@RunWith(MockitoJUnitRunner.class)
public class AutoCreateOxoRejectTest2 extends AbstractRestApiTest {

    private ShipmentReadLogic shipmentReadLogic;

    private OrderReadLogic orderReadLogic;

    private HKShipmentDoneLogic hKShipmentDoneLogic;

    private CompensateBizLogic compensateBizLogic;

    @InjectMocks
    private AutoCreateRejectOrderHandler autoCreateRejectOrderHandler;

    private OrderShipmentReadService orderShipmentReadService;

    private ShipmentWriteService shipmentWriteService;

    private RefundReadLogic refundReadLogic;

    private MiddleOrderFlowPicker flowPicker;

    private RefundWriteLogic refundWriteLogic;

    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ShipmentReadLogic shipmentReadLogic;

        @MockBean
        private OrderReadLogic orderReadLogic;

        @MockBean
        private HKShipmentDoneLogic hKShipmentDoneLogic;

        @MockBean
        private CompensateBizLogic compensateBizLogic;

        @MockBean
        private OrderShipmentReadService orderShipmentReadService;

        @MockBean
        private ShipmentWriteService shipmentWriteService;

        @MockBean
        private RefundReadLogic refundReadLogic;

        @MockBean
        private MiddleOrderFlowPicker flowPicker;

        @MockBean
        private RefundWriteLogic refundWriteLogic;
    }

    @Override
    protected void init() {
        shipmentReadLogic = get(ShipmentReadLogic.class);
        orderReadLogic = get(OrderReadLogic.class);
        hKShipmentDoneLogic = get(HKShipmentDoneLogic.class);
        compensateBizLogic = get(CompensateBizLogic.class);
        orderShipmentReadService = get(OrderShipmentReadService.class);
        shipmentWriteService = get(ShipmentWriteService.class);
        refundReadLogic = get(RefundReadLogic.class);
        flowPicker = get(MiddleOrderFlowPicker.class);
        refundWriteLogic = get(RefundWriteLogic.class);

        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(autoCreateRejectOrderHandler, "sendLock", false);
        ReflectionTestUtils.setField(autoCreateRejectOrderHandler, "oxoAutoCreateRejectRetry", 1);
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Test
    public void autoCreateRejectOrderHandler() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setShipmentCode("SHP1111111");
        shipment.setStatus(MiddleShipmentsStatus.WAIT_SHIP.getValue());
        shipment.setShipmentCorpCode("xx");
        shipment.setShipmentCorpName("xx");
        shipment.setShipmentSerialNo("xx");
        shipment.setExtra(Maps.newHashMap());

        OrderShipment orderShipment = new OrderShipment();
        orderShipment.setId(1L);
        when(shipmentReadLogic.findOrderShipmentByShipmentId(anyLong())).thenReturn(orderShipment);

        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setOutFrom(MiddleChannel.VIPOXO.getValue());
        shopOrder.setOrderCode("xxx");
        shopOrder.setStatus(MiddleOrderStatus.CANCEL_FAILED.getValue());
        when(orderReadLogic.findShopOrderById(anyLong())).thenReturn(shopOrder);

        //校验有没有已经生成了拒收单

        List<Refund> refunds = Lists.newArrayList();
        when(refundReadLogic.findRefundsByOrderId(anyLong())).thenReturn(refunds);

        List<ShipmentItem> shipmentItems = Lists.newArrayList();
        ShipmentItem shipmentItem = new ShipmentItem();
        shipmentItem.setId(1L);
        shipmentItem.setSkuOrderId(1L);
        shipmentItem.setSkuCode("111");
        shipmentItem.setQuantity(1);
        shipmentItem.setShipQuantity(1);
        shipmentItem.setItemId("1");
        shipmentItem.setCleanFee(100);
        shipmentItems.add(shipmentItem);
        when(shipmentReadLogic.getShipmentItems(any(Shipment.class))).thenReturn(shipmentItems);

        when(refundWriteLogic.createRefund(any(SubmitRefundInfo.class))).thenThrow(new RuntimeException("xxx"));

        autoCreateRejectOrderHandler.autoCreateRejectOrder(shipment);
    }
}