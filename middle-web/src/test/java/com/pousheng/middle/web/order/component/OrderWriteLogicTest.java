package com.pousheng.middle.web.order.component;

import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.Shipments;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

public class OrderWriteLogicTest extends AbstractRestApiTest {

    private OrderWriteLogic orderWriteLogic;
    private MiddleOrderFlowPicker flowPicker;
    private OrderWriteService orderWriteService;
    private MiddleOrderWriteService middleOrderWriteService;
    private ShipmentReadService shipmentReadService;
    private OrderReadLogic orderReadLogic;
    private ShipmentWiteLogic shipmentWiteLogic;
    private ShipmentReadLogic shipmentReadLogic;
    private SyncShipmentLogic syncShipmentLogic;
    private OrderServiceCenter orderServiceCenter;
    private MiddleOrderReadService middleOrderReadService;
    private OpenShopReadService openShopReadService;
    private ExpressCodeReadService expressCodeReadService;
    private Flow orderFlow;

    @Configuration
    public static class MockitoBeans {
        @SpyBean
        private OrderWriteLogic orderWriteLogic;
        @MockBean
        private MiddleOrderFlowPicker flowPicker;
        @MockBean
        private OrderWriteService orderWriteService;
        @MockBean
        private MiddleOrderWriteService middleOrderWriteService;
        @MockBean
        private ShipmentReadService shipmentReadService;
        @MockBean
        private OrderReadLogic orderReadLogic;
        @MockBean
        private ShipmentWiteLogic shipmentWiteLogic;
        @MockBean
        private ShipmentReadLogic shipmentReadLogic;
        @MockBean
        private SyncShipmentLogic syncShipmentLogic;
        @MockBean
        private OrderServiceCenter orderServiceCenter;
        @MockBean
        private MiddleOrderReadService middleOrderReadService;
        @MockBean
        private OpenShopReadService openShopReadService;
        @MockBean
        private ExpressCodeReadService expressCodeReadService;
        @MockBean
        private Flow orderFlow;
    }

    @Override
    protected void init() {
        orderWriteLogic = get(OrderWriteLogic.class);
        flowPicker = get(MiddleOrderFlowPicker.class);
        orderWriteService = get(OrderWriteService.class);
        middleOrderWriteService = get(MiddleOrderWriteService.class);
        shipmentReadService = get(ShipmentReadService.class);
        orderReadLogic = get(OrderReadLogic.class);
        shipmentWiteLogic = get(ShipmentWiteLogic.class);
        shipmentReadLogic = get(ShipmentReadLogic.class);
        syncShipmentLogic = get(SyncShipmentLogic.class);
        orderServiceCenter = get(OrderServiceCenter.class);
        middleOrderReadService = get(MiddleOrderReadService.class);
        openShopReadService = get(OpenShopReadService.class);
        expressCodeReadService = get(ExpressCodeReadService.class);
        orderFlow = get(Flow.class);
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Test
    public void rollbackShopOrder() {
        Long shopOrderId = 259l;

        ShopOrder shopOrder = new  ShopOrder();
        List<SkuOrder> skuOrders = new ArrayList<SkuOrder>();
        Response<List<Shipment>> shipmentsRes = new Response<List<Shipment>>();
        Shipment shipment = new Shipment();
        List failSkuOrders = new ArrayList();

        when(orderReadLogic.findShopOrderById(anyLong())).thenReturn(shopOrder);
        when(orderReadLogic.findSkuOrderByShopOrderIdAndStatus(anyLong(), any())).thenReturn(skuOrders);
        when(shipmentReadService.findByOrderIdAndOrderLevel(any(), any())).thenReturn(shipmentsRes);
        when(shipmentWiteLogic.cancelShipment(shipment, 1)).thenReturn(Response.ok(false));
        when(middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder, failSkuOrders,MiddleOrderEvent.REVOKE_FAIL.toOrderOperation())).thenReturn(null);
        when(middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder, skuOrders,MiddleOrderEvent.REVOKE_FAIL.toOrderOperation())).thenReturn(null);
        //when(orderReadLogic.getOrderExtraMapValueByKey(any(),any())).thenReturn(ecpStatus);

        //todo validateRollbackShopOrder 私有方法测试有点问题,待重新实现测试
        //when(orderFlow.operationAllowed(any(),any())).thenReturn(true);
        //ReflectionTestUtils.invokeMethod(orderWriteLogic,"validateRollbackShopOrder", shopOrder);
        //orderWriteLogic.rollbackShopOrder(shopOrderId);
    }
}