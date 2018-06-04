package com.pousheng.middle.web.biz.impl;

import com.jd.open.api.sdk.domain.ECLP.EclpOpenService.ShipperOut;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class TmallCreateShipmentServiceTest extends AbstractRestApiTest {

    private TmallCreateShipmentService tmallCreateShipmentService;
    private OrderReadLogic orderReadLogic;
    private ShipmentWiteLogic shipmentWiteLogic;
    private OrderWriteService orderWriteService;
    private MiddleOrderWriteService middleOrderWriteService;

    @Configuration
    public static class MockitoBeans {
        @SpyBean
        private TmallCreateShipmentService tmallCreateShipmentService;
        @MockBean
        private OrderReadLogic orderReadLogic;
        @MockBean
        private ShipmentWiteLogic shipmentWiteLogic;
        @MockBean
        private OrderWriteService orderWriteService;
        @MockBean
        private MiddleOrderWriteService middleOrderWriteService;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        tmallCreateShipmentService = get(TmallCreateShipmentService.class);
        orderReadLogic = get(OrderReadLogic.class);
        shipmentWiteLogic =get(ShipmentWiteLogic.class);
        orderWriteService =get(OrderWriteService.class);
        middleOrderWriteService = get(MiddleOrderWriteService.class);
    }

    @Test
    public void doProcess() {

        ShopOrder shopOrder = new ShopOrder();
        when(middleOrderWriteService.updateHandleStatus(any(),any(),any())).thenReturn(Response.ok(Boolean.TRUE));
        when(orderReadLogic.findShopOrderById(any())).thenReturn(shopOrder);
        doThrow(new RuntimeException()).when(shipmentWiteLogic).toDispatchOrder(shopOrder);
        when(orderWriteService.updateOrderExtra(any(),any(),any())).thenReturn(Response.ok(Boolean.TRUE));
        doThrow(new RuntimeException()).when(shipmentWiteLogic).toDispatchOrder(shopOrder);

        Long shopOrderId = 100l;
        String jsonStr = JsonMapper.nonEmptyMapper().toJson(shopOrderId);

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.TMALL_ORDER_CREATE_SHIP.toString());
        biz.setContext(jsonStr);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        tmallCreateShipmentService.doProcess(biz);
    }
}
