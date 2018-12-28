package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.dto.ShipmentExtra;
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
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.service.ShipmentWriteService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.Matchers.any;
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

    public static final String SHIP_INFO_STR1="{\"shipmentId\":\"SHP1708254\","
        + "\"yyEDIShipmentId\":\"ESL65991305126196\",\"shipmentCorpCode\":\"PINJUN\","
        + "\"shipmentSerialNo\":\"PJ7571000091010\",\"shipmentDate\":\"20190104170018\",\"weight\":374.4800000000001,"
        + "\"expectDate\":\"20190104235900\",\"transportMethodCode\":\"2\",\"transportMethodName\":\"空运\","
        + "\"cardRemark\":\"匡威\",\"itemInfos\":[{\"skuCode\":\"6902014539450\",\"quantity\":2,"
        + "\"shipmentCorpCode\":\"PINJUN\",\"shipmentSerialNo\":\"PJ7571000091010\","
        + "\"boxNo\":\"329019010400000074\"},{\"skuCode\":\"6902014539450\",\"quantity\":2,"
        + "\"shipmentCorpCode\":\"PINJUN\",\"shipmentSerialNo\":\"PJ7571000091010\","
        + "\"boxNo\":\"329019010400000075\"},{\"skuCode\":\"6902014539450\",\"quantity\":1,"
        + "\"shipmentCorpCode\":\"PINJUN\",\"shipmentSerialNo\":\"PJ7571000091010\",\"boxNo\":\"329019010400000076\"}]}";

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


    @Test
    public void oneBiz() {
        YyEdiShipInfo yyEdiShipInfo=JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(SHIP_INFO_STR1,YyEdiShipInfo.class);

        List<ShipmentItem> itemList=JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(ITEM_LIST_MOCK_STR,
            JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class,ShipmentItem.class));
        when(shipmentReadLogic.getShipmentItems(any())).thenReturn(itemList);

        ShipmentExtra shipmentExtra = new ShipmentExtra();
        when(shipmentReadLogic.getShipmentExtra(any())).thenReturn(shipmentExtra);
        //yyediSyncShipmentService.oneBiz(yyEdiShipInfo);
    }



    public String ITEM_LIST_MOCK_STR="[{\"id\":1206190,\"shipmentId\":1708254,\"warehouseId\":18076,\"shopId\":1167,"
        + "\"status\":5,\"skuOrderId\":1799064,\"skuCode\":\"6902014539450\",\"outSkuCode\":null,"
        + "\"skuOutId\":\"19010339081018_4377630\",\"skuName\":\"CONVERSE(匡威)CONVERSE ALL STAR系列男硫化鞋CS162054\","
        + "\"itemId\":null,\"cleanPrice\":32594,\"cleanFee\":32594,\"refundQuantity\":0,\"quantity\":2,"
        + "\"shipQuantity\":0,\"integral\":0,\"skuPrice\":32594,\"skuDiscount\":0,\"isGift\":false,"
        + "\"sharePlatformDiscount\":0,\"createdAt\":1546568415000,\"updatedAt\":1546592478000,\"careStock\":0,"
        + "\"itemStock\":0,\"itemWarehouseName\":\"宝胜青浦SDC-京东A类总部大宗销售仓\"},{\"id\":1206191,\"shipmentId\":1708254,"
        + "\"warehouseId\":18076,\"shopId\":1167,\"status\":5,\"skuOrderId\":1799064,\"skuCode\":\"6902014539450\","
        + "\"outSkuCode\":null,\"skuOutId\":\"19010339081018_4377630\",\"skuName\":\"CONVERSE(匡威)CONVERSE ALL "
        + "STAR系列男硫化鞋CS162054\",\"itemId\":null,\"cleanPrice\":32594,\"cleanFee\":32594,\"refundQuantity\":0,"
        + "\"quantity\":2,\"shipQuantity\":0,\"integral\":0,\"skuPrice\":32594,\"skuDiscount\":0,\"isGift\":false,"
        + "\"sharePlatformDiscount\":0,\"createdAt\":1546568415000,\"updatedAt\":1546592478000,\"careStock\":0,"
        + "\"itemStock\":0,\"itemWarehouseName\":\"宝胜青浦SDC-京东A类总部大宗销售仓\"},{\"id\":1206192,\"shipmentId\":1708254,"
        + "\"warehouseId\":18076,\"shopId\":1167,\"status\":5,\"skuOrderId\":1799064,\"skuCode\":\"6902014539450\","
        + "\"outSkuCode\":null,\"skuOutId\":\"19010339081018_4377630\",\"skuName\":\"CONVERSE(匡威)CONVERSE ALL "
        + "STAR系列男硫化鞋CS162054\",\"itemId\":null,\"cleanPrice\":32594,\"cleanFee\":32594,\"refundQuantity\":0,"
        + "\"quantity\":2,\"shipQuantity\":0,\"integral\":0,\"skuPrice\":32594,\"skuDiscount\":0,\"isGift\":false,"
        + "\"sharePlatformDiscount\":0,\"createdAt\":1546568415000,\"updatedAt\":1546592478000,\"careStock\":0,"
        + "\"itemStock\":0,\"itemWarehouseName\":\"宝胜青浦SDC-京东A类总部大宗销售仓\"}]";
}