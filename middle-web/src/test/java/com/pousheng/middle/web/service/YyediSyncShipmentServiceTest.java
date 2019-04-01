/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: YyediSyncShipmentServiceTest
 * Author:   xiehong
 * Date:     2018/5/29 下午10:50
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.web.service;

import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.impl.YyediSyncShipmentService;
import com.pousheng.middle.web.order.component.HKShipmentDoneLogic;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentWriteService;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * @author xiehong
 * @create 2018/5/29 下午10:50
 */
public class YyediSyncShipmentServiceTest extends AbstractRestApiTest {


    @Configuration
    public static class MockitoBeans {

        @MockBean
        private ShipmentReadLogic shipmentReadLogic;

        @MockBean
        private MiddleOrderFlowPicker flowPicker;

        @MockBean
        private OrderReadLogic orderReadLogic;

        @MockBean
        private SyncShipmentPosLogic syncShipmentPosLogic;

        @MockBean
        private HKShipmentDoneLogic hKShipmentDoneLogic;
        @MockBean
        private ShipmentWriteService shipmentWriteService;


        @SpyBean
        private YyediSyncShipmentService yyediSyncShipmentService;


    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        shipmentReadLogic = get(ShipmentReadLogic.class);
        flowPicker = get(MiddleOrderFlowPicker.class);
        orderReadLogic = get(OrderReadLogic.class);
        syncShipmentPosLogic = get(SyncShipmentPosLogic.class);
        hKShipmentDoneLogic = get(HKShipmentDoneLogic.class);
        yyediSyncShipmentService = get(YyediSyncShipmentService.class);
        shipmentWriteService = get(ShipmentWriteService.class);

    }

    ShipmentReadLogic shipmentReadLogic;
    MiddleOrderFlowPicker flowPicker;
    OrderReadLogic orderReadLogic;
    SyncShipmentPosLogic syncShipmentPosLogic;
    HKShipmentDoneLogic hKShipmentDoneLogic;
    YyediSyncShipmentService yyediSyncShipmentService;
    ShipmentWriteService shipmentWriteService;


    @Test
    public void doProcess() {

        when(shipmentReadLogic.findShipmentByShipmentCode(anyString())).thenReturn(new Shipment() {{
            setId(123L);
        }});
        when(flowPicker.pickShipments()).thenReturn(MiddleFlowBook.shipmentsFlow);
        when(shipmentWriteService.updateStatusByShipmentId(anyLong(), anyInt())).thenReturn(Response.ok());
        when(shipmentReadLogic.getShipmentExtra(any())).thenReturn(new ShipmentExtra());
        when(shipmentWriteService.update(any())).thenReturn(Response.ok());
        when(syncShipmentPosLogic.syncShipmentPosToHk(any())).thenReturn(Response.ok());

        String data = "[{\"shipmentId\":\"1000221232312\",\"yyEDIShipmentId\":\"2341342132222\",\"shipmentCorpCode\":\"123\",\"shipmentSerialNo\":\"21312333333\",\"shipmentDate\":\"20180912\",\"weight\":12}]";

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.YYEDI_SYNC_SHIPMENT_RESULT.toString());
        biz.setContext(data);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        yyediSyncShipmentService.doProcess(biz);

    }


}