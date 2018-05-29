/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: YyEDIOpenApiTest
 * Author:   xiehong
 * Date:     2018/5/29 下午9:27
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.open.api;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author xiehong
 * @create 2018/5/29 下午9:27
 */
public class YyEDIOpenApiTest extends AbstractRestApiTest {

    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ShipmentReadLogic shipmentReadLogic;

        @MockBean
        private MiddleOrderFlowPicker flowPicker;

        @MockBean
        private RefundReadLogic refundReadLogic;
        @MockBean
        private RefundWriteLogic refundWriteLogic;
        @MockBean
        private AutoCompensateLogic autoCompensateLogic;
        @MockBean
        private SyncRefundPosLogic syncRefundPosLogic;
        @MockBean
        private EventBus eventBus;
        @MockBean
        private SyncShipmentPosLogic syncShipmentPosLogic;

        @MockBean
        private ReceiveYyediResultLogic receiveYyediResultLogic;

        @SpyBean
        private yyEDIOpenApi yyEDIOpenApi;

    }

    ShipmentReadLogic shipmentReadLogic;
    MiddleOrderFlowPicker flowPicker;
    ReceiveYyediResultLogic receiveYyediResultLogic;
    yyEDIOpenApi yyEDIOpenApi;

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        shipmentReadLogic = get(ShipmentReadLogic.class);
        yyEDIOpenApi = get(yyEDIOpenApi.class);
        flowPicker  = get(MiddleOrderFlowPicker.class);
        receiveYyediResultLogic  = get(ReceiveYyediResultLogic.class);

    }


    @Test
    public void receiveYYEDIShipmentResult(){

    when(shipmentReadLogic.findShipmentByShipmentCode(anyString())).thenReturn(new Shipment(){{setStatus(4);}});
    when(flowPicker.pickShipments()).thenReturn(MiddleFlowBook.shipmentsFlow);
    when(receiveYyediResultLogic.createShipmentResultTask(anyList())).thenReturn(Response.ok());
    String data = "[{\"shipmentId\":\"1000221232312\",\"yyEDIShipmentId\":\"2341342132222\",\"shipmentCorpCode\":\"123\",\"shipmentSerialNo\":\"21312333333\",\"shipmentDate\":\"20180912\",\"weight\":12}]";
    yyEDIOpenApi.receiveYYEDIShipmentResult(data);
    }

}