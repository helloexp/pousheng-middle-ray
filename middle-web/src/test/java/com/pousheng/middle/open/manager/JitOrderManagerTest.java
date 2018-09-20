package com.pousheng.middle.open.manager;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.parana.order.impl.manager.OrderManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class JitOrderManagerTest {

    @Mock
    private OrderManager orderManager;

    @Mock
    private OpenShopReadService openShopReadService;

    @Mock
    private OpenShopCacher openShopCacher;

    @Mock
    private WarehouseCacher warehouseCacher;

    @Mock
    private InventoryClient inventoryClient;
    @InjectMocks
    private JitOrderManager jitOrderManager;

    private OpenShop openShop;

    private OpenFullOrderInfo openFullOrderInfo;

    private WarehouseDTO mockWarehouse;

    private Response<List<AvailableInventoryDTO>> inventorySuccessResponse;

    private String testData="{\n"
        + "            \"order\":{\n"
        + "                \"outOrderId\":\"123456\",\n"
        + "                \"buyerName\":\"张三\",\n"
        + "                \"buyerMobile\":\"13616144345\",\n"
        + "                \"companyCode\":\"244\",\n"
        + "                \"outId\":\"244-123456\",\n"
        + "                \"shopCode\":\"D2CC3C38E0FACB05E6702FBA2E1F9D9C\",\n"
        + "                \"fee\":10000,\n"
        + "                \"originFee\":18000,\n"
        + "                \"shipFee\":1000,\n"
        + "                \"originShipFee\":\"1000\",\n"
        + "                \"discount\":8000,\n"
        + "                \"integral\":0,\n"
        + "                \"shipmentType\":1,\n"
        + "                \"payType\":1,\n"
        + "                \"orderChannel\":1,\n"
        + "                \"type\":2,\n"
        + "                \"buyerNote\":\"要发顺丰\",\n"
        + "                \"status\":1,\n"
        + "                \"channel\":\"yunjujit\",\n"
        + "                \"paymentChannelName\":\"支付宝\",\n"
        + "                \"paymentSerialNo\":\"13133213\",\n"
        + "                \"paymentDate\":\"20180111120910\",\n"
        + "                \"createdAt\":\"20180111120920\",\n"
        + "                \"stockId\":\"244-244000003\",\n"
        + "                \"isCareStock\":\"Y\",\n"
        + "                \"isSyncHk\":\"N\",\n"
        + "                \"interStockCode\":\"DD\",\n"
        + "                \"preFinishBillo\":\"DDD\",\n"
        + "                \"batchNo\":\"1\",\n"
        + "                \"batchMark\":\"D\",\n"
        + "                \"channelCode\":\"yunjujit\"\n"
        + "            },\n"
        + "            \"item\":[\n"
        + "                {\n"
        + "                    \"outSkuorderId\":\"1\",\n"
        + "                    \"skuCode\":\"OPER08222XL\",\n"
        + "                    \"itemType\":\"01\",\n"
        + "                    \"itemName\":\"addias小童系列\",\n"
        + "                    \"quantity\":2,\n"
        + "                    \"originFee\":9000,\n"
        + "                    \"discount\":2500,\n"
        + "                    \"cleanPrice\":15000,\n"
        + "                    \"cleanFee\":9000\n"
        + "                },\n"
        + "                {\n"
        + "                    \"outSkuorderId\":\"1\",\n"
        + "                    \"skuCode\":\"4049067087407\",\n"
        + "                    \"itemType\":\"01\",\n"
        + "                    \"itemName\":\"addias小童系列\",\n"
        + "                    \"quantity\":2,\n"
        + "                    \"originFee\":9000,\n"
        + "                    \"discount\":4500,\n"
        + "                    \"cleanPrice\":15000,\n"
        + "                    \"cleanFee\":9000\n"
        + "                }\n"
        + "            ],\n"
        + "            \"invoice\":{\n"
        + "                \"invoiceType\":\"1\",\n"
        + "                \"titleType\":\"1\"\n"
        + "            },\n"
        + "            \"address\":{\n"
        + "                \"receiveUserName\":\"王二\",\n"
        + "                \"mobile\":\"15890456790\",\n"
        + "                \"phone\":\"02589761000\",\n"
        + "                \"email\":\"wanger@126.com\",\n"
        + "                \"province\":\"浙江省\",\n"
        + "                \"city\":\"杭州市\",\n"
        + "                \"region\":\"滨江区\",\n"
        + "                \"street\":\"浦沿街道\",\n"
        + "                \"detail\":\"六合路中控大厦E22\",\n"
        + "                \"postcode\":\"214590\"\n"
        + "            }\n"
        + "        }";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        openFullOrderInfo= JsonMapper.nonEmptyMapper().fromJson(testData,OpenFullOrderInfo.class);
        openShop=new OpenShop();
        when(openShopCacher.findById(anyLong())).thenReturn(openShop);

        mockWarehouse=new WarehouseDTO();
        when(warehouseCacher.findByCode(anyString())).thenReturn(mockWarehouse);
    }

    @Test
    public void batchHandleRealTimeOrder() {
        Response<List<OpenClientShop>> mockShopResponse=new Response<>();
        mockShopResponse.setSuccess(true);
        List<OpenClientShop> mockShopList=Lists.newArrayList(new OpenClientShop());
        mockShopResponse.setResult(mockShopList);

        when(openShopReadService.search(anyString(),anyString(),anyString())).thenReturn(mockShopResponse);
        //List<OpenFullOrderInfo> orders= Lists.newArrayList();
        //OpenFullOrderInfo info=new OpenFullOrderInfo();
        //OpenFullOrder order=new OpenFullOrder();
        //order.setChannel(JitConsts.YUN_JU_JIT);
        //info.setOrder(order);
        //orders.add(info);
        //jitOrderManager.batchHandleRealTimeOrder(Lists.newArrayList(openFullOrderInfo));
    }

    @Test
    public void handleRealTimeOrder() {
    }

    @Test
    public void handleReceiveOrder() {
    }

    @Test
    public void saveOrderAndLockInventory() {
    }

    @Test
    public void validateInventory() {
    }

    @Test
    public void lockInventory() {
    }

    @Test
    public void saveUnlockInventoryTask() {
    }
}