/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: NotifyHkOrderDoneServiceTest
 * Author:   xiehong
 * Date:     2018/5/30 下午5:38
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.web.service;

import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.web.biz.impl.NotifyHkOrderDoneService;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.erp.SyncErpShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

/**
 *
 * @author xiehong
 * @create 2018/5/30 下午5:38
 */
public class NotifyHkOrderDoneServiceTest extends AbstractRestApiTest {

    @Configuration
    public static class MockitoBeans {

        @MockBean
        private ShipmentReadLogic shipmentReadLogic;
        @MockBean
        private PoushengSettlementPosReadService poushengSettlementPosReadService;
        @MockBean
        private PoushengSettlementPosWriteService poushengSettlementPosWriteService;
        @MockBean
        private ShipmentWiteLogic shipmentWiteLogic;
        @MockBean
        private SyncErpShipmentLogic syncErpShipmentLogic;
        @MockBean
        private SyncShipmentPosLogic syncShipmentPosLogic;
        @MockBean
        private AutoCompensateLogic autoCompensateLogic;

        @SpyBean
        private NotifyHkOrderDoneService notifyHkOrderDoneService;


    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        shipmentReadLogic = get(ShipmentReadLogic.class);
        poushengSettlementPosReadService = get(PoushengSettlementPosReadService.class);
        poushengSettlementPosWriteService = get(PoushengSettlementPosWriteService.class);
        shipmentWiteLogic = get(ShipmentWiteLogic.class);
        syncErpShipmentLogic = get(SyncErpShipmentLogic.class);
        syncShipmentPosLogic = get(SyncShipmentPosLogic.class);
        autoCompensateLogic = get(AutoCompensateLogic.class);
        notifyHkOrderDoneService = get(NotifyHkOrderDoneService.class);

    }

    ShipmentReadLogic shipmentReadLogic;
    PoushengSettlementPosReadService poushengSettlementPosReadService;
    PoushengSettlementPosWriteService poushengSettlementPosWriteService;
    ShipmentWiteLogic shipmentWiteLogic;
    SyncErpShipmentLogic syncErpShipmentLogic;
    SyncShipmentPosLogic syncShipmentPosLogic;
    AutoCompensateLogic autoCompensateLogic;
    NotifyHkOrderDoneService notifyHkOrderDoneService;


    @Test
    public void doProcess() {

        when(shipmentReadLogic.findByOrderIdAndType(anyLong())).thenReturn(new ArrayList<OrderShipment>(){{add(new OrderShipment(){{setStatus(5);}});}});
        when(shipmentReadLogic.findShipmentById(anyLong())).thenReturn(new Shipment(){{setId(1234L);}});
        when(shipmentReadLogic.getShipmentExtra(any())).thenReturn(new ShipmentExtra(){{setShipmentWay("2");}});
        when(syncErpShipmentLogic.syncShipmentDone(any(),any(),any())).thenReturn(Response.ok());
        when(poushengSettlementPosReadService.findByShipmentId(anyLong())).thenReturn(Response.ok(new PoushengSettlementPos(){{setId(21342L);}}));
        when(poushengSettlementPosWriteService.update(any())).thenReturn(Response.ok());

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.YYEDI_SYNC_SHIPMENT_RESULT.toString());
        biz.setContext("12312312312");
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        notifyHkOrderDoneService.doProcess(biz);

    }


}