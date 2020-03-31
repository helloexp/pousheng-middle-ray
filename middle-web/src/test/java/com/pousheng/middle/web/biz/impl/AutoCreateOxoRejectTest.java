package com.pousheng.middle.web.biz.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.SubmitRefundInfo;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.web.order.component.*;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ShipmentWriteService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author zhurg
 * @date 2019/6/19 - 上午10:20
 */
@RunWith(MockitoJUnitRunner.class)
public class AutoCreateOxoRejectTest extends AbstractRestApiTest {

    @InjectMocks
    private YyediSyncShipmentService yyediSyncShipmentService;

    private ShipmentReadLogic shipmentReadLogic;

    private OrderReadLogic orderReadLogic;

    private HKShipmentDoneLogic hKShipmentDoneLogic;

    private CompensateBizLogic compensateBizLogic;

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
        private AutoCreateRejectOrderHandler autoCreateRejectOrderHandler;

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
        autoCreateRejectOrderHandler = get(AutoCreateRejectOrderHandler.class);
        orderShipmentReadService = get(OrderShipmentReadService.class);
        shipmentWriteService = get(ShipmentWriteService.class);
        refundReadLogic = get(RefundReadLogic.class);
        flowPicker = get(MiddleOrderFlowPicker.class);
        refundWriteLogic = get(RefundWriteLogic.class);

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void process() {
        PoushengCompensateBiz poushengCompensateBiz = new PoushengCompensateBiz();
        poushengCompensateBiz.setId(1L);

        List<YyEdiShipInfo> yyEdiShipInfos = Lists.newArrayList();

        YyEdiShipInfo yyEdiShipInfo = new YyEdiShipInfo();
        yyEdiShipInfo.setShipmentId("SHP1111111");
        yyEdiShipInfo.setYyEDIShipmentId("oxoxoxoxox");
        List<YyEdiShipInfo.ItemInfo> itemInfos = Lists.newArrayList();
        YyEdiShipInfo.ItemInfo itemInfo = new YyEdiShipInfo.ItemInfo();
        itemInfo.setQuantity(1);
        itemInfo.setShipmentCorpCode("xx");
        itemInfo.setShipmentSerialNo("xx");
        itemInfo.setSkuCode("111");
        itemInfo.setBoxNo("1");
        yyEdiShipInfo.setItemInfos(itemInfos);

        yyEdiShipInfos.add(yyEdiShipInfo);

        poushengCompensateBiz.setContext(JSON.toJSONString(yyEdiShipInfos));
        poushengCompensateBiz.setStatus("WAIT_HANDLE");

        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setShipmentCode("SHP1111111");
        shipment.setStatus(MiddleShipmentsStatus.WAIT_SHIP.getValue());
        shipment.setShipmentCorpCode("xx");
        shipment.setShipmentCorpName("xx");
        shipment.setShipmentSerialNo("xx");
        shipment.setExtra(Maps.newHashMap());
        when(shipmentReadLogic.findShipmentByShipmentCode(anyString())).thenReturn(shipment);

        when(flowPicker.pickShipments()).thenReturn(MiddleFlowBook.shipmentsFlow);

        List<ShipmentItem> shipmentItems = Lists.newArrayList();
        ShipmentItem shipmentItem = new ShipmentItem();
        shipmentItem.setId(1L);
        shipmentItem.setSkuOrderId(1L);
        shipmentItem.setSkuCode("111");
        shipmentItem.setQuantity(1);
        shipmentItem.setShipQuantity(1);
        shipmentItem.setItemId("1");
        shipmentItem.setCleanFee(100);
        when(shipmentReadLogic.getShipmentItems(any(Shipment.class))).thenReturn(shipmentItems);

        ShipmentExtra shipmentExtra = new ShipmentExtra();
        Map<Long, Map<String, Integer>> assignBoxTable = Maps.newHashMap();
        Map<String, Integer> row = Maps.newHashMap();
        row.put("1", 1);
        assignBoxTable.put(1L, row);
        shipmentExtra.setAssignBoxDetail(assignBoxTable);
        when(shipmentReadLogic.getShipmentExtra(any(Shipment.class))).thenReturn(shipmentExtra);

        Response<OrderShipment> orderShipmentResponse = new Response<>();
        orderShipmentResponse.setSuccess(true);
        OrderShipment orderShipment = new OrderShipment();
        orderShipment.setId(1L);
        orderShipmentResponse.setResult(orderShipment);
        when(orderShipmentReadService.findByShipmentId(anyLong())).thenReturn(orderShipmentResponse);

        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setOutFrom(MiddleChannel.YUNJUJIT.getValue());
        shopOrder.setOrderCode("xxx");
        when(orderReadLogic.findShopOrderById(anyLong())).thenReturn(shopOrder);

        Response<Boolean> response = new Response<>();
        response.setSuccess(true);
        when(shipmentWriteService.updateStatusByShipmentId(anyLong(), anyInt())).thenReturn(response);

        ExpressCode expressCode = new ExpressCode();
        expressCode.setName("xx");
        when(orderReadLogic.makeExpressNameByhkCode(anyString())).thenReturn(expressCode);

        when(shipmentWriteService.update(any(Shipment.class))).thenReturn(response);

        doNothing().when(hKShipmentDoneLogic).doneShipment(any(Shipment.class));
        when(compensateBizLogic.createBizAndSendMq(any(PoushengCompensateBiz.class), anyString())).thenReturn(1L);

        doCallRealMethod().when(autoCreateRejectOrderHandler).autoCreateRejectOrder(any(Shipment.class));

        when(shipmentReadLogic.findOrderShipmentByShipmentId(anyLong())).thenReturn(orderShipment);
        when(orderReadLogic.findShopOrderById(anyLong())).thenReturn(shopOrder);

        //校验有没有已经生成了拒收单

        List<Refund> refunds = Lists.newArrayList();
        when(refundReadLogic.findRefundsByOrderId(anyLong())).thenReturn(refunds);

        when(refundWriteLogic.createRefund(any(SubmitRefundInfo.class))).thenReturn(1L);

        yyediSyncShipmentService.doProcess(poushengCompensateBiz);
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }
}