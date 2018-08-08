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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.mockito.Matchers.*;
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
    RefundReadLogic refundReadLogic;
    RefundWriteLogic refundWriteLogic;
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
        refundReadLogic  = get(RefundReadLogic.class);
        refundWriteLogic  = get(RefundWriteLogic.class);

    }


    @Test
    public void receiveYYEDIShipmentResult(){

        when(shipmentReadLogic.findShipmentByShipmentCode(anyString())).thenReturn(new Shipment(){{setStatus(4);}});
        when(flowPicker.pickShipments()).thenReturn(MiddleFlowBook.shipmentsFlow);
        when(receiveYyediResultLogic.createShipmentResultTask(anyList())).thenReturn(Response.ok());
        String data = "[{\"shipmentId\":\"1000221232312\",\"yyEDIShipmentId\":\"2341342132222\",\"shipmentCorpCode\":\"123\",\"shipmentSerialNo\":\"21312333333\",\"shipmentDate\":\"20180912\",\"weight\":12}]";
        yyEDIOpenApi.receiveYYEDIShipmentResult(data);
    }

     @Test
    public void syncHkRefundStatus(){

        when(refundReadLogic.findRefundByRefundCode(anyString())).thenReturn(new Refund(){{setRefundType(2);setId(123L);setExtra(Maps.newHashMap());}});
        when(refundReadLogic.findRefundExtra(any())).thenReturn(new RefundExtra());
        when(refundWriteLogic.updateStatus(any(),any())).thenReturn(Response.ok());
        when(receiveYyediResultLogic.createRefundStatusTask(anyList())).thenReturn(Response.ok());
        String data = "{\"itemCode\":\"1000221232312\",\"warhouseCode\":\"123\",\"quantity\":\"123\"}";
        yyEDIOpenApi.syncHkRefundStatus("123","12312",data,"20180531151212");
    }


    @Test
    public void convertToYyEdiShipInfo() {
        String shipInfo = "[\n" +
                "\t{\n" +
                "\t\t\"shipmentId\": \"1223132\",\n" +
                "\t\t\"yjShipmentId\": \"100\",\n" +
                "\t\t\"shipmentCorpCode\": \"YTO\",\n" +
                "\t\t\"shipmentSerialNo\": \"1245\",\n" +
                "\t\t\"shipmentDate\": \"20160625224210\",\n" +
                "\t\t\"weight\": \"74\",\n" +
                "\t\t\"itemInfos\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"skuCode\": \"0001\",\n" +
                "\t\t\t\t\"quantity\": \"2\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"skuCode\": \"0001\",\n" +
                "\t\t\t\t\"quantity\": \"3\"\n" +
                "\t\t\t}\n" +
                "\t\t]\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"shipmentId\": \"1223112\",\n" +
                "\t\t\"yjShipmentId\": \"2\",\n" +
                "\t\t\"shipmentCorpCode\": \"YTO\",\n" +
                "\t\t\"shipmentSerialNo\": \"12425\",\n" +
                "\t\t\"shipmentDate\": \"20160625224210\",\n" +
                "\t\t\"weight\": \"74\",\n" +
                "\t\t\"itemInfos\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"skuCode\": \"0001\",\n" +
                "\t\t\t\t\"quantity\": \"2\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"skuCode\": \"0001\",\n" +
                "\t\t\t\t\"quantity\": \"3\"\n" +
                "\t\t\t}\n" +
                "\t\t]\n" +
                "\t}\n" +
                "]";
        List<YyEdiShipInfo> results = JsonMapper.nonEmptyMapper().fromJson(shipInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YyEdiShipInfo.class));
        System.out.print(results);

    }
}