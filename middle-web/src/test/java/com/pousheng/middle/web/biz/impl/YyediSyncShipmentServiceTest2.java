package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.OrderShipmentWriteService;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.HKShipmentDoneLogic;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.vip.SyncVIPLogic;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentWriteService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class YyediSyncShipmentServiceTest2 {
    @InjectMocks
    private YyediSyncShipmentService yyediSyncShipmentService;

    @Mock
    private ShipmentReadLogic shipmentReadLogic;


    @Mock
    private OrderReadLogic orderReadLogic;

    @Mock
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Mock
    private HKShipmentDoneLogic hKShipmentDoneLogic;

    @Mock
    private ShipmentWriteService shipmentWriteService;

    @Mock
    private OrderShipmentWriteService orderShipmentWriteService;

    @Mock
    private AutoCompensateLogic autoCompensateLogic;

    @Mock
    private SyncVIPLogic syncVIPLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Mock
    private OrderShipmentReadService orderShipmentReadService;

    @Mock
    private CompensateBizLogic compensateBizLogic;


    @Before
    public void init(){
        Shipment shipment=new Shipment();
        shipment.setStatus(MiddleShipmentsStatus.WAIT_SHIP.getValue());
        when(shipmentReadLogic.findShipmentByShipmentCode(anyString())).thenReturn(shipment);
    }

    @Test
    public void doProcess() {

        PoushengCompensateBiz biz=new PoushengCompensateBiz();
        String context="[{\"cardRemark\":\"阿迪达斯\",\"expectDate\":\"20181113160000\","
            + "\"itemInfos\":[{\"boxNo\":\"61128218111200000057\",\"quantity\":1,\"shipmentCorpCode\":\"PINJUN\","
            + "\"shipmentSerialNo\":\"7024000009416\",\"skuCode\":\"4059807677096\"},"
            + "{\"boxNo\":\"61128218111200000057\",\"quantity\":1,\"shipmentCorpCode\":\"PINJUN\","
            + "\"shipmentSerialNo\":\"7024000009416\",\"skuCode\":\"4059807892888\"},"
            + "{\"boxNo\":\"61128218111200000057\",\"quantity\":1,\"shipmentCorpCode\":\"PINJUN\","
            + "\"shipmentSerialNo\":\"7024000009416\",\"skuCode\":\"4059807892932\"}],"
            + "\"shipmentCorpCode\":\"PINJUN\",\"shipmentDate\":\"20181112143107\",\"shipmentId\":\"SHP1228110\","
            + "\"shipmentSerialNo\":\"7024000009416\",\"transportMethodCode\":\"2\",\"transportMethodName\":\"空运\","
            + "\"weight\":137.34,\"yyEDIShipmentId\":\"ESL65991245083042\"}]";
        biz.setContext(context);
        yyediSyncShipmentService.doProcess(biz);
    }
}