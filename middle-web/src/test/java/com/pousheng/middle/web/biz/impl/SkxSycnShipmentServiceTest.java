package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentWriteService;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/2上午11:48
 */
public class SkxSycnShipmentServiceTest extends AbstractRestApiTest {

    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ShipmentReadLogic shipmentReadLogic;

        @MockBean
        private MiddleOrderFlowPicker flowPicker;

        @MockBean
        private SyncShipmentPosLogic syncShipmentPosLogic;

        @MockBean
        private HKShipmentDoneLogic hkShipmentDoneLogic;

        @MockBean
        private AutoCompensateLogic autoCompensateLogic;

        @MockBean
        private OrderReadLogic orderReadLogic;

        @MockBean
        private ShipmentWriteService shipmentWriteService;

        @SpyBean
        private SkxSycnShipmentService skxSycnShipmentService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    ShipmentReadLogic shipmentReadLogic;
    MiddleOrderFlowPicker flowPicker;
    SyncShipmentPosLogic syncShipmentPosLogic;
    HKShipmentDoneLogic hkShipmentDoneLogic;
    AutoCompensateLogic autoCompensateLogic;
    OrderReadLogic orderReadLogic;
    ShipmentWriteService shipmentWriteService;
    SkxSycnShipmentService skxSycnShipmentService;

    @Override
    protected void init() {
        shipmentReadLogic = get(ShipmentReadLogic.class);
        flowPicker = get(MiddleOrderFlowPicker.class);
        syncShipmentPosLogic = get(SyncShipmentPosLogic.class);
        hkShipmentDoneLogic = get(HKShipmentDoneLogic.class);
        autoCompensateLogic = get(AutoCompensateLogic.class);
        orderReadLogic = get(OrderReadLogic.class);
        shipmentWriteService = get(ShipmentWriteService.class);
        skxSycnShipmentService = get(SkxSycnShipmentService.class);
    }


    @Test
    public void doBiz () {
        when(shipmentReadLogic.findShipmentByShipmentCode(anyString())).thenReturn(new Shipment() {{
            setId(123L);
        }});
        when(flowPicker.pickShipments()).thenReturn(MiddleFlowBook.shipmentsFlow);
        when(shipmentWriteService.updateStatusByShipmentId(anyLong(), anyInt())).thenReturn(Response.ok());
        when(shipmentReadLogic.getShipmentExtra(any())).thenReturn(new ShipmentExtra());
        when(shipmentWriteService.update(any())).thenReturn(Response.ok());
        when(syncShipmentPosLogic.syncShipmentPosToHk(any())).thenReturn(Response.ok());


        String data = "{\"shipmentId\":\"1000221232312\",\"erpShipmentId\":\"2341342132222\",\"shipmentCorpCode\":\"123\",\"shipmentSerialNo\":\"21312333333\",\"shipmentDate\":\"20180912120000\"}";

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.SKX_SYNC_SHIPMENT_RESULT.toString());
        biz.setContext(data);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        skxSycnShipmentService.doProcess(biz);
    }

}
