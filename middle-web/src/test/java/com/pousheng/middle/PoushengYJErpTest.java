package com.pousheng.middle;

import com.alibaba.fastjson.JSONObject;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.pousheng.middle.hksyc.dto.JitOrderReceiptRequest;
import com.pousheng.middle.hksyc.dto.YJSyncRejectRefundCancelRequest;
import com.pousheng.middle.hksyc.dto.YJSyncShipmentRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.yyedisyc.dto.trade.*;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderAddress;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.open.client.order.dto.OpenFullOrderItem;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Throwables;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Charsets.UTF_8;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/2下午3:54
 */
@Slf4j
public class PoushengYJErpTest {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String SID = "PS_ERP_WMS_wmsjitdeliver";
    /**
     * 同步发货单到云聚ERP
     */
    @Test
    public void doSyncYJErpShipmentOrder() {
        List<YJErpShipmentInfo> requestData = Lists.newArrayList();
        YJErpShipmentInfo yjErpShipmentInfo = new YJErpShipmentInfo();
        // 外部订单号（中台的发货单号) Y
        yjErpShipmentInfo.setOther_order_sn("SHP12345");
        // 收货人 Y
        yjErpShipmentInfo.setConsignee("连云港");
        // 省份 (编码) Y
        yjErpShipmentInfo.setProvince("110000");
        // 城市 (编码) Y
        yjErpShipmentInfo.setCity("110100");
        // 区域 (编码)
        yjErpShipmentInfo.setArea("110101");
        // 传0验证province、city、area
        // 传1验证province_name、city_name、area_name
        yjErpShipmentInfo.setCheck_area(1);
        // 省 Y
        yjErpShipmentInfo.setProvince_name("北京");
        // 市 Y
        yjErpShipmentInfo.setCity_name("北京市");
        // 区 Y
        yjErpShipmentInfo.setArea_name("东城区");
        // 联系地址 Y
        yjErpShipmentInfo.setAddress("银河soho D座");
        // 邮编
        yjErpShipmentInfo.setZipcode("110100");
        // 联系电话
        yjErpShipmentInfo.setTelephone("010-1234567");
        // 手机号码 Y
        yjErpShipmentInfo.setMobile("13241587777");
        // 备注信息
        yjErpShipmentInfo.setMessage("请发申通，谢谢");
        // 是否有货先发，参数（ 有货商品可以先发货:1，等待所有商品到货一起发货:2）
        // 默认 1
        yjErpShipmentInfo.setDelivery_option(1);
        // 寄件人
        yjErpShipmentInfo.setSender("中台");
        // 寄件人电话
        yjErpShipmentInfo.setSender_mobile("13212345678");
        // 寄件人地址
        yjErpShipmentInfo.setSender_address("上海");
        // 保价金额
        yjErpShipmentInfo.setInsured_price(100.00F);
        // 代收金额
        yjErpShipmentInfo.setCollection_amount(1000.00F);
        // 快递公司ID
        yjErpShipmentInfo.setLogistics_company_id(null);
        // 快递单号
        yjErpShipmentInfo.setLogistics_order("");
        // Y
        // 配送方式 1 货到付款 2 普通快递 3 顺丰快递 4 EMS
        // 5 顺丰到付 6 物流配送(到付) 7 B2B2C分销自提 8 B2B2C团购（拣后付费) 9 京东配送
        yjErpShipmentInfo.setDelivery_type_id(2);
        // 是否开发票，参数（1是，0否） 默认 0
        yjErpShipmentInfo.setIs_invoice(0);
        // 发票类型，参数（不开发票 0，公司 1，个人 2） 默认 0
        yjErpShipmentInfo.setInvoice_type(null);
        // 发票抬头
        yjErpShipmentInfo.setInvoice_title("");


        List<YJErpShipmentProductInfo> product_info = Lists.newArrayList();
        YJErpShipmentProductInfo yjErpShipmentProductInfo = new YJErpShipmentProductInfo();
        yjErpShipmentProductInfo.setGoods_code("1PU30510302");
        yjErpShipmentProductInfo.setSize("13");
        yjErpShipmentProductInfo.setBar_code("4053984439560");
        yjErpShipmentProductInfo.setNum(2);
        yjErpShipmentProductInfo.setWarehouse_code("WH310255");
        product_info.add(yjErpShipmentProductInfo);

        // 订单商品 Y
        yjErpShipmentInfo.setProduct_list(product_info);

        requestData.add(yjErpShipmentInfo);
        doSyncYJErpShipmentOrder(requestData);
    }


    public String doSyncYJErpShipmentOrder(List<YJErpShipmentInfo> requestData) {
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        System.out.println("sync shipment to yj erp paramJson:" + paramJson);
        String gateway ="https://esbt.pousheng.com/common-yjerp/yjerp/default/pushmgorderset";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode","b82d30f3f1fc4e43b3f427ba3d7b9a50")
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .trustAllHosts()
                .trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        System.out.println("sync shipment to yj erp result:" + responseBody);
        return responseBody;
    }



    /**
     * 取消发货单到云聚ERP
     */
    @Test
    public void doYJErpCancelOrder () {
        List<YJErpCancelInfo> requestData = Lists.newArrayList();
        YJErpCancelInfo yjErpCancelInfo = new YJErpCancelInfo();

        // 外部订单号（中台的发货单号) Y
        yjErpCancelInfo.setOther_order_sn("SHP123");
        // 库房code Y
        yjErpCancelInfo.setWarehouse_code("WH310255");

        requestData.add(yjErpCancelInfo);
        doYJErpCancelOrder(requestData);
    }


    public String doYJErpCancelOrder(List<YJErpCancelInfo> requestData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        log.info("sync cancel shipment to yj erp paramJson :{}",paramJson);
        String gateway = "https://esbt.pousheng.com/common-yjerp/yjerp/default/pushmgordercancel";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode","b82d30f3f1fc4e43b3f427ba3d7b9a50")
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .trustAllCerts()
                .trustAllHosts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync cancel shipment to yj erp result:{}",responseBody);
        return responseBody;
    }


    /**
     * 同步售后单到云聚ERP
     */
    @Test
    public void doSyncYJErpRefundOrder() {
        List<YJErpRefundInfo> requestData = Lists.newArrayList();
        YJErpRefundInfo yjErpRefundInfo = new YJErpRefundInfo();

        // 联系人名称
        yjErpRefundInfo.setConsignee("中台");
        // 联系人手机号
        yjErpRefundInfo.setMobile("13200001234");
        // 退货快递单号 Y
        yjErpRefundInfo.setExpress_num("12345678");
        // 备注信息
        yjErpRefundInfo.setMessage("退货");
        // 中台退货单号 Y
        yjErpRefundInfo.setMg_exchange_sn("mg_1234567");
        // 订单号 中台发货单号 Y
        yjErpRefundInfo.setOrder_sn("300593950");
        // 库房code
        yjErpRefundInfo.setWarehouse_code("WH310255");

        List<YJErpRefundProductInfo> list = Lists.newArrayList();
        YJErpRefundProductInfo yjErpRefundProductInfo = new YJErpRefundProductInfo();
        yjErpRefundProductInfo.setBar_code("4053984439539");
        yjErpRefundProductInfo.setNum(1);
        yjErpRefundProductInfo.setExchange_reason_id(2);
        yjErpRefundProductInfo.setAppearance(1);
        yjErpRefundProductInfo.setPackaging(1);
        yjErpRefundProductInfo.setProblem_type(0);
        list.add(yjErpRefundProductInfo);

        // 退货商品信息 Y
        yjErpRefundInfo.setProduct_info(list);
        requestData.add(yjErpRefundInfo);
        doSyncYJErpRefundOrder(requestData);

    }

    public String doSyncYJErpRefundOrder(List<YJErpRefundInfo> requestData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);;
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        log.info("sync refund to yj erp paramJson:{}",paramJson);
        String gateway ="https://esbt.pousheng.com/common-yjerp/yjerp/default/pushmgorderexchangeset";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode","b82d30f3f1fc4e43b3f427ba3d7b9a50")
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .trustAllHosts()
                .trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync refund to yj erp result:{}",responseBody);
        return responseBody;
    }

    public void post(String url,Map<String, Object> params){
        String result = HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).form(params).body();
        System.out.println(result);
    }


    /**
     * 云聚erp回执中台
     */
    @Test
    public void testSyncOrder() {
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", "pousheng");
        params.put("pampasCall", "yj.shipments.api");
        // 参数构造
        List<YyEdiShipInfo> results = Lists.newArrayList();
        YyEdiShipInfo yyEdiShipInfo = new YyEdiShipInfo();
        yyEdiShipInfo.setShipmentId("1223132");
        yyEdiShipInfo.setYjShipmentId("1");
        yyEdiShipInfo.setShipmentCorpCode("YTO");
        yyEdiShipInfo.setShipmentSerialNo("1245");
        yyEdiShipInfo.setShipmentDate("20180625224210");
        yyEdiShipInfo.setWeight(74);
        List<YyEdiShipInfo.ItemInfo> itemInfos = Lists.newArrayList();
        YyEdiShipInfo.ItemInfo itemInfo1 = new YyEdiShipInfo.ItemInfo();
        itemInfo1.setSkuCode("0001");
        itemInfo1.setQuantity(2);
        itemInfo1.setShipmentCorpCode("YTO");
        itemInfo1.setShipmentSerialNo("1245");

        YyEdiShipInfo.ItemInfo itemInfo2 = new YyEdiShipInfo.ItemInfo();
        itemInfo2.setSkuCode("0002");
        itemInfo2.setQuantity(3);
        itemInfo2.setShipmentCorpCode("YTO");
        itemInfo2.setShipmentSerialNo("2345");

        itemInfos.add(itemInfo1);
        itemInfos.add(itemInfo2);
        yyEdiShipInfo.setItemInfos(itemInfos);
        results.add(yyEdiShipInfo);

        String data = mapper.toJson(results);
        log.info("data:{}", data);
        params.put("shipInfo", data);


        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, Charsets.UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", Charsets.UTF_8).hash().toString();
        params.put("sign", sign);

        //post("http://127.0.0.1:8092/api/gateway",params);

        post("http://middle-api-test.pousheng.com/api/gateway", params);
    }

    /**
     * 云聚erp售后单回执中台
     */
    @Test
    public void testYyediRefundReturn(){
        Map<String, Object> params = Maps.newTreeMap();

        params.put("appKey", "pousheng");
        params.put("pampasCall", "yj.refund.confirm.received.api");

        params.put("refundOrderId","19233");
        params.put("yjRefundOrderId","74");
        params.put("receivedDate",DateTimeFormat.forPattern("yyyyMMddHHmmss").print(new DateTime()));

        YYEdiRefundConfirmItem confirmItem = new YYEdiRefundConfirmItem();
        confirmItem.setItemCode("0001");
        confirmItem.setQuantity("2");
        confirmItem.setWarhouseCode("0001");

        params.put("itemInfo",mapper.toJson(Lists.newArrayList(confirmItem)));

        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", UTF_8).hash().toString();
        params.put("sign", sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

        // post("http://127.0.0.1:8092/api/gateway", params);
        post("http://middle-api-test.pousheng.com/api/gateway", params);
    }

    @Test
    public void convertJson(){
        String info = "{\"error\":0,\"error_info\":\"\\u8ba2\\u5355\\u5199\\u5165\\u6210\\u529f\",\"data\":{\"order_sn\":\"300593950\"}}";
        JSONObject responseObj = JSONObject.parseObject(info);
        System.out.print(responseObj.getString("error_info"));
        if (Objects.equals(responseObj.get("error"), 0)) {
            JSONObject data = JSONObject.parseObject(responseObj.getString("data"));
            System.out.print(data.getString("order_sn"));
        }
    }


    @Test
    public void doSyncShipmentOrder(){

        WmsShipmentInfo requestData=new WmsShipmentInfo();
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

        WmsShipmentInfoRequest request = new WmsShipmentInfoRequest();
        request.setSid(SID);
        String now = DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN));
        request.setTranReqDate(now);
        request.setBizContent(requestData);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(request);
        log.info("sync shipment to wms erp paramJson:{}",paramJson);
        String gateway ="https://esbt.pousheng.com/common/pserp/wms/wmsjitdeliver";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode","b82d30f3f1fc4e43b3f427ba3d7b9a50")
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync shipment to wms erp result:{}",responseBody);
    }

    @Test
    public void aaa(){


//        String str="{\"address\":{\"city\":\"北京市\",\"detail\":\"银河soho\",\"mobile\":\"13700000000\","
//            + "\"province\":\"北京\",\"receiveUserName\":\"唯品会\",\"region\":\"东城区\"},\"invoice\":{},"
//            + "\"item\":[{\"cleanFee\":1,\"cleanPrice\":1,\"discount\":0,\"originFee\":1,"
//            + "\"outSkuorderId\":\"308971541_886736621999\",\"quantity\":\"1\",\"skuCode\":\"886736621999\"}],"
//            + "\"order\":{\"buyerName\":\"唯品会\",\"channel\":\"yunjurt\",\"companyCode\":\"300\","
//            + "\"createdAt\":\"20180915100838\",\"fee\":1,\"isCareStock\":\"N\",\"isSyncHk\":\"N\",\"originFee\":1,"
//            + "\"originShipFee\":0,\"outOrderId\":\"308971541+40\",\"payType\":1,\"shipFee\":0,\"shipmentType\":1,"
//            + "\"shopCode\":\"11310202\",\"status\":1,\"stockId\":\"300-300000325\"}}";
//

        for(int i = 1; i < 2; i++) {
            Map<String, Object> params = Maps.newTreeMap();
            params.put("appKey", "pousheng");
            params.put("pampasCall", "push.out.jit.order.api");

            OpenFullOrderInfo fullOrderInfo = new OpenFullOrderInfo();
            OpenFullOrder order = new OpenFullOrder();
            order.setBatchMark("第三批次");
            order.setBatchNo("3");
            order.setCardRemark("耐克");
            order.setBuyerName("宋仁飞");
            order.setChannel("yunjujit");
            order.setChannelCode("yunjujit");
            order.setCompanyCode("300");
            order.setCreatedAt("20180925155435");
            order.setExpectDate("2018-09-26 09:00:00");
            order.setFee(1L);
            order.setInterStockCode("VIP_SH");
            order.setIsCareStock("Y");
            order.setIsSyncHk("N");
            order.setJitOrderId("PICK-60015001");
            order.setOriginFee(1L);
            order.setOriginShipFee(0L);
            order.setOutId("PICK-60025002");
            order.setOutOrderId("6035000+200"+i);
            order.setPayType(1);
            order.setPreFinishBillo("6015000_100");
            StringBuffer sb = new StringBuffer();
//            for(int j = 1; j <= 20; j++){
//                String temp = "";
//                if(j == 20) {
//                    temp = "9950001+100" + j;
//                }
//                else{
//                    temp = "9950001+100" + j +",";
//                }
//                sb.append(temp);
//
//            }
            order.setRealtimeOrderIds(sb.toString());
            order.setShipFee(0L);
            order.setShipmentType(1);
            order.setShopCode("11310202");
            order.setStatus(1);
            order.setStockId("301-301011164");
            order.setType(2);
            order.setTransportMethodCode("2");
            order.setTransportMethodName("ss");
            fullOrderInfo.setOrder(order);

            OpenFullOrderAddress address = new OpenFullOrderAddress();
            address.setCity("北京市");
            address.setDetail("5000单测试数据");
            address.setMobile("13700000000");
            address.setProvince("province");
            address.setReceiveUserName("唯品会5000");
            address.setRegion("东城区");
            fullOrderInfo.setAddress(address);


            List<OpenFullOrderItem> items = Lists.newArrayList();
            for(int j = 1; j <= 5000; j++) {
                OpenFullOrderItem item = new OpenFullOrderItem();
                item.setCleanFee(1L);
                item.setCleanPrice(1L);
                item.setDiscount(0L);
                item.setItemType("01");
                item.setOriginFee(1L);
                item.setOutSkuorderId("6035000_000" + j);
                item.setQuantity(1);
                item.setSkuCode("4058031902707");
                item.setVipsOrderId("6035000_000" + j);
                items.add(item);


            }

            fullOrderInfo.setItem(items);

            params.put("orderInfo", mapper.toJson(fullOrderInfo));


            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, Charsets.UTF_8)
                    .putString("6a0e@93204aefe45d47f6e488", Charsets.UTF_8).hash().toString();
            params.put("sign", sign);

            System.out.println(mapper.toJson(params));

            //post("http://127.0.0.1:8092/api/gateway",params);

            //post("http://middle-api-test.pousheng.com/api/gateway", params);
            post("http://middle-api-prepub.pousheng.com/api/gateway", params);
        }
    }


    @Test
    public void sendJitOrderReceipt(){
        JitOrderReceiptRequest receiptRequest=new JitOrderReceiptRequest();
        receiptRequest.setOrder_sn("302197428+103");
        receiptRequest.setError_code(1);
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);;
        String paramJson = JsonMapper.nonEmptyMapper().toJson(receiptRequest);
        log.info("send jit order receipt paramJson:{}",paramJson);
        String gateway ="https://esbt.pousheng.com/common-yjerp/yjerp/default/pushmgorderdealreturn";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode","b82d30f3f1fc4e43b3f427ba3d7b9a50")
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .trustAllHosts()
                .trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("send jit order receipt. result:{}",responseBody);
    }

    @Test
    public void doSyncJitShipmentOrder() {
        YJSyncShipmentRequest syncShipmentRequest=new YJSyncShipmentRequest();
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

        String paramJson = JsonMapper.nonEmptyMapper().toJson(syncShipmentRequest);
        log.info("doSyncShipmentOrder to yj jit order id:{} paramJson:{}", syncShipmentRequest.getOrder_sn(),paramJson);
        String uri = "https://esbt.pousheng.com/common-yjerp/yjerp/default/pushmgviporderout";
        String responseBody = null;
        try {
            responseBody = HttpRequest.post(uri)
                    .header("verifycode", "b82d30f3f1fc4e43b3f427ba3d7b9a50")
                    .header("serialNo", serialNo)
                    .header("sendTime", DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                    .contentType("application/json")
                    .trustAllHosts().trustAllCerts()
                    .send(paramJson)
                    .connectTimeout(1000000).readTimeout(1000000)
                    .body();

            log.info("rpc yunju jit mgorderout out order id:{}  responseBody={}",syncShipmentRequest.getOrder_sn(), responseBody);

        } catch (Exception e) {
            log.error("rpc yunju jit mgorderout out order id:{} exception happens,exception={}",syncShipmentRequest.getOrder_sn(), Throwables
                    .getStackTrace(e));
        }

    }

    @Test
    public void testCancelJitOrder(){
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", "pousheng");
        params.put("pampasCall", "out.order.cancel.api");

        //CancelOutOrderInfo info=new CancelOutOrderInfo();
        //info.setOutOrderId("");
        //params.put("data", str);


        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, Charsets.UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", Charsets.UTF_8).hash().toString();
        params.put("sign", sign);

        //post("http://127.0.0.1:8092/api/gateway",params);

        post("http://middle-api-test.pousheng.com/api/gateway", params);
    }

    @Test
    public void testWMSjitShipment(){
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", "pousheng");
        params.put("pampasCall", "jit.shipments.api");

        String shipInfo="[{\"cardRemark\":\"\",\"expectDate\":\"20180918235900\",\"itemInfos\":[],"
                + "\"shipmentCorpCode\":\"品骏\",\"shipmentDate\":\"\",\"shipmentId\":\"SHP8659292\","
                + "\"shipmentSerialNo\":\"\",\"transportMethodCode\":\"1\",\"transportMethodName\":\"汽运\",\"weight\":0.0,"
                + "\"yyEDIShipmentId\":\"1809X0028\"}]";
        params.put("shipInfo", shipInfo);


        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, Charsets.UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", Charsets.UTF_8).hash().toString();
        params.put("sign", sign);

        post("http://127.0.0.1:8092/api/gateway",params);

        //post("http://middle-api-test.pousheng.com/api/gateway", params);
    }

    @Test
    public void testYjRejectRefundCancel(){
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        YJSyncRejectRefundCancelRequest request = YJSyncRejectRefundCancelRequest.builder().mg_exchange_sn("ASS1000837").build();
        String paramJson = JsonMapper.nonEmptyMapper().toJson(request);
        String gateway = "http://esbpp.pousheng.com/common-yjerp/bw/yjerp/pushmgexchangecancelreturn";
        String responseBody = null;
        try {
            responseBody = HttpRequest.post(gateway)
                    .header("verifycode","b6ca4adc4c2c4375a1ff70745d57d5eb")
                    .header("serialNo", serialNo)
                    .header("sendTime", DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                    .contentType("application/json")
                    .send(paramJson)
                    .connectTimeout(10000).readTimeout(10000)
                    .body();

            log.info("yunju mgexchangecancelreturn mg_exchange_sn:{} responseBody={}", request.getMg_exchange_sn(), responseBody);
        } catch (Exception e) {
            log.error("yunju mgexchangecancelreturn mg_exchange_sn:{} exception happens,exception={}", request.getMg_exchange_sn(), Throwables.getStackTrace(e));
        }
    }

}
