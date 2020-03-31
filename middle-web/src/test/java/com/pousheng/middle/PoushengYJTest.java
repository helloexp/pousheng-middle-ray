package com.pousheng.middle;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.pousheng.middle.open.api.dto.*;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.enums.MiddleChannel;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderAddress;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.open.client.order.dto.OpenFullOrderItem;
import io.terminus.parana.order.model.ShipmentItem;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Created by songrenfei on 17/3/
 */
@Slf4j
public class PoushengYJTest {


    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    protected Map<String, Object> params = Maps.newTreeMap();

    private final static DateTimeFormatter DFT2 = DateTimeFormat.forPattern("yyyyMMddHHmmss");


    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 对参数列表进行签名
     */
    public String sign(String secret) {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);

            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, UTF_8)
                    .putString(secret, UTF_8).hash().toString();

            return sign;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String url() {
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String url = "http://127.0.0.1:8090/api/gateway" + "?" + suffix;
        System.out.println(url);
        return url;
    }

    public String gateway() {
        return "http://127.0.0.1:8090/api/gateway";
    }

    public String url(String gateway) {
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String url = gateway + "?" + suffix;
        System.out.println(url);
        return url;
    }


    public void post(String url, Map<String, Object> params) {
        String result = HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).form(params).body();
        System.err.println(result);
    }

    public String get(String url) {
        String result = HttpRequest.get(url).connectTimeout(1000000).readTimeout(1000000).body();
        return result;
    }


    public void get(String url, Map<String, Object> params) {
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String urlStr = url + "?" + suffix;
        System.out.println("request url:" + urlStr);
        String result = HttpRequest.get(urlStr).connectTimeout(1000000).readTimeout(1000000).body();
        System.out.println(result);
    }

    @Test
    public void testSyncOrder(){
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey","pousheng");
        params.put("pampasCall","push.out.open.order.api");
        List<OpenFullOrderInfo> orderInfos = Lists.newArrayList();
        OpenFullOrderInfo openFullOrderInfo = new OpenFullOrderInfo();

        OpenFullOrder openFullOrder = new OpenFullOrder();
        openFullOrder.setBuyerName("qiantan");
        openFullOrder.setChannel("yunjubbc");
        openFullOrder.setCompanyCode("300");
        openFullOrder.setCreatedAt("20180101123302");
        openFullOrder.setFee(10000L);
        openFullOrder.setOriginFee(10000L);
        openFullOrder.setShipFee(0L);
        openFullOrder.setOutOrderId("3367839+121");
        openFullOrder.setOriginShipFee(0L);
        openFullOrder.setShipmentType(1);
        openFullOrder.setPayType(1);
        openFullOrder.setShopCode("13000997");
        openFullOrder.setStatus(1);
        openFullOrder.setStockId("300-300000325");

        // 50个子订单
        List<OpenFullOrderItem> items =  Lists.newArrayList();
//        for (int i = 0; i < 1; i++) {
            OpenFullOrderItem item = new OpenFullOrderItem();
            item.setOutSkuorderId("236703963+111_677778");
            item.setSkuCode("4053984439560");
            item.setItemType("01");
            item.setItemName("测试商品");
            item.setQuantity(4);
            item.setOriginFee(1000L);
            item.setDiscount(0L);
            item.setCleanPrice(1000L);
            item.setCleanFee(4000L);
            items.add(item);

        OpenFullOrderItem item1 = new OpenFullOrderItem();
        item1.setOutSkuorderId("236781963+111_677778");
        item1.setSkuCode("6903313008203");
        item1.setItemType("02");
        item1.setItemName("测试商品");
        item1.setQuantity(3);
        item1.setOriginFee(2000L);
        item1.setDiscount(0L);
        item1.setCleanPrice(2000L);
        item1.setCleanFee(6000L);
        items.add(item1);
//        }

        OpenFullOrderAddress address = new OpenFullOrderAddress();
        address.setProvince("江苏省");
        address.setCity("南京市");
        address.setRegion("江宁区");
        address.setDetail("胜利路89号");
        address.setMobile("18021529596");
        address.setPhone("02512345678");
        address.setReceiveUserName("易秋涵");

        openFullOrderInfo.setOrder(openFullOrder);
        openFullOrderInfo.setItem(items);
        openFullOrderInfo.setAddress(address);
        orderInfos.add(openFullOrderInfo);
        String data = JsonMapper.nonEmptyMapper().toJson(orderInfos);
        log.info("order",data);
        params.put("orderInfo",data);
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, Charsets.UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", Charsets.UTF_8).hash().toString();
        params.put("sign",sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

        //post("http://127.0.0.1:8092/api/gateway",params);

        post("http://middle-api-test.pousheng.com/api/gateway",params);
//        post("http://middle-api-prepub.pousheng.com/api/gateway",params);
    }


    @Test
    public void testCancelOrder() {
        Map<String, Object> params = Maps.newTreeMap();

        params.put("appKey", "pousheng");
        params.put("pampasCall", "out.order.cancel.api");

        CancelOutOrderInfo cancelOutOrderInfo = new CancelOutOrderInfo();
        cancelOutOrderInfo.setChannel("yunjubbc");
        cancelOutOrderInfo.setApplyAt(DFT2.print(new DateTime()));
        cancelOutOrderInfo.setBuyerNote("尺码小了，不合适");
        cancelOutOrderInfo.setSellerNote("同意买家申请");
        cancelOutOrderInfo.setOutOrderId("296783063+121");
        cancelOutOrderInfo.setOutSkuOrderId("");

        String data = mapper.toJson(cancelOutOrderInfo);
        log.info("data:{}", data);
        params.put("data", data);
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", UTF_8).hash().toString();
        params.put("sign", sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

//        post("http://127.0.0.1:8092/api/gateway", params);

        post("http://middle-api-test.pousheng.com/api/gateway",params);
    }


    @Test
    public void testRefund() {
        Map<String, Object> params = Maps.newTreeMap();

        params.put("appKey", "pousheng");
        params.put("pampasCall", "out.order.refund.api");


        OutRefundOrder refundOrder = new OutRefundOrder();
        refundOrder.setOutAfterSaleOrderId("1001");
        refundOrder.setType(2);
        refundOrder.setFee(400L);
        refundOrder.setApplyAt(DFT2.print(new DateTime()));
        refundOrder.setBuyerMobile("13093771726");
        refundOrder.setSellerNote("商家备注");
        refundOrder.setBuyerName("freefei");
        refundOrder.setBuyerNote("买家备注");
        refundOrder.setExpressCode("code0003");
        refundOrder.setStatus(1);
        refundOrder.setOutOrderId("0002");

        List<OutOrderRefundItem> refundItems = Lists.newArrayList();
        OutOrderRefundItem refundItem = new OutOrderRefundItem();
        refundItem.setFee(400L);
        refundItem.setItemName("商品名称001");
        refundItem.setAfterSaleId("1001");
        refundItem.setSkuAfterSaleId("2001");
        refundItem.setQuantity(23);
        refundItem.setSkuCode("00003");

        refundItems.add(refundItem);

        OutOrderApplyRefund applyRefund = new OutOrderApplyRefund();
        applyRefund.setItems(refundItems);
        applyRefund.setRefund(refundOrder);

        String data = mapper.toJson(applyRefund);
        log.info("data:{}", data);

        String reqString="{\"items\":[{\"afterSaleId\":\"2509\",\"fee\":319,\"skuAfterSaleId\":\"2509_2849018\",\"skuCode\":\"4057289618996\"},{\"afterSaleId\":\n" +
                "\"2509\",\"fee\":319,\"skuAfterSaleId\":\"2509_2849017\",\"skuCode\":\"4057289618903\"}],\"refund\":{\"applyAt\":\"20180423 204056\",\"buyerName\"\n" +
                ":\"小强\",\"outAfterSaleOrderId\":\"2509\",\"outOrderId\":\"296132768+3\",\"returnStockid\":\"244-244000069\",\"status\":1,\"type\":2}}";
        params.put("data", reqString);

        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", UTF_8).hash().toString();
        params.put("sign", sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

        post("http://127.0.0.1:8092/api/gateway", params);

        //post("http://middle-api-test.pousheng.com/api/gateway",params);
    }


    @Test
    public void testreceiveYYEDIShipmentResult() {

        Map<String, Object> params = Maps.newTreeMap();

        params.put("appKey", "pousheng");
        params.put("pampasCall", "yyEDI.shipments.api");


        YyEdiShipInfo yyEdiShipInfo = new YyEdiShipInfo();

        yyEdiShipInfo.setWeight(100L);
        yyEdiShipInfo.setShipmentCorpCode("STO");
        yyEdiShipInfo.setYyEDIShipmentId("7777777");
        yyEdiShipInfo.setShipmentSerialNo("yyedei777777");
        yyEdiShipInfo.setShipmentDate(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(new DateTime()));
        yyEdiShipInfo.setShipmentId("SHP102744"); //shipmentcode
        ArrayList<YyEdiShipInfo> yyEdiShipInfos = Lists.newArrayList(yyEdiShipInfo);

        String data = mapper.toJson(yyEdiShipInfos);
        log.info("data:{}", data);
        params.put("shipInfo", data);
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", UTF_8).hash().toString();
        params.put("sign", sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

        post("http://127.0.0.1:8092/api/gateway", params);

    }

    @Test
    public void method(){
        System.out.println(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(new DateTime()));
    }


    @Test
    public void testCreatYunJuRefund(){

        Map<String, Object> params = Maps.newTreeMap();

        params.put("appKey", "pousheng");
        params.put("pampasCall", "out.order.refund.api");

        OutOrderApplyRefund orderApplyRefund = new OutOrderApplyRefund();

        OutRefundOrder refundOrder = new OutRefundOrder();
        refundOrder.setOutOrderId("991"); ///云聚单号
        refundOrder.setOutAfterSaleOrderId("888888");
        refundOrder.setBuyerName("buyer-name");
        refundOrder.setType(2);
        refundOrder.setStatus(1);
        refundOrder.setApplyAt("20180417 165101");
        refundOrder.setExpressCode("expresscode-002");
        refundOrder.setChannel(MiddleChannel.YUNJUBBC.getValue());
        ArrayList<OutOrderRefundItem> items = Lists.newArrayList();
        OutOrderRefundItem item = new OutOrderRefundItem();
        item.setAfterSaleId("888888");
        item.setSkuCode("WNM0822M");
        item.setFee(4032L);
        item.setSkuAfterSaleId("555555"); //子售后单id
        item.setQuantity(2);
        items.add(item);
        orderApplyRefund.setRefund(refundOrder);
        orderApplyRefund.setItems(items);

        String parmJson = "{\"items\":[{\"afterSaleId\":\"2508\",\"fee\":319.2,\"skuAfterSaleId\":\"2508_2849018\",\"skuCode\":\"4057289618996\"},{\"afterSaleId\":\"2508\",\"fee\":319.2,\"skuAfterSaleId\":\"2508_2849017\",\"skuCode\":\"4057289618903\"}],\"refund\":{\"applyAt\":\"20180417 165316\",\"buyerName\":\"小强\",\"expressCode\":\"test_1234567\",\"outAfterSaleOrderId\":\"2508\",\"outOrderId\":\"296132768+3\",\"returnStockid\":\"244-244000069\",\"status\":1,\"type\":2}}";
        String data = mapper.toJson(orderApplyRefund);
        log.info("data:{}", data);
        params.put("data", parmJson);
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", UTF_8).hash().toString();
        params.put("sign", sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

        post("http://127.0.0.1:8092/api/gateway", params);

    }


    @Test
    public void testYyediRefundReturn(){
        Map<String, Object> params = Maps.newTreeMap();

        params.put("appKey", "pousheng");
        params.put("pampasCall", "yyEDI.refund.confirm.received.api");

        params.put("refundOrderId","ASS265408");
        params.put("yyEDIRefundOrderId","yyedi_refund_no"+System.currentTimeMillis());
        YYEdiRefundConfirmItem confirmItem = new YYEdiRefundConfirmItem();

        confirmItem.setItemCode("886737551462");
        confirmItem.setQuantity("1");
        confirmItem.setWarhouseCode("20");


        params.put("itemInfo",mapper.toJson(Lists.newArrayList(confirmItem)));
        params.put("receivedDate",DateTimeFormat.forPattern("yyyyMMddHHmmss").print(new DateTime()));
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
                .putString(toVerify, UTF_8)
                .putString("6a0e@93204aefe45d47f6e488", UTF_8).hash().toString();
        params.put("sign", sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

        post("http://127.0.0.1:8095/api/gateway", params);
    }

    @Test
    public void method22() throws UnsupportedEncodingException {
        String info = "["
                + "{"
                + "\"skuCode\":\"6903313017076\","
                + "\"skuName\":\"CONVERSE(匡威)CONVERSE ALL STAR系列中性硫化鞋101000\","
                + "\"cleanPrice\":39800,"
                + "\"cleanFee\":39800,"
                + "\"attrs\":"
                + "["
                + "{"
                + "\"attrKey\":\"颜色\","
                + "\"attrVal\":\"红色\","
                + "\"showImage\":false"
                + "},"
                + "{"
                + "\"attrKey\":\"尺码\","
                + "\"attrVal\":\"4\","
                + "\"showImage\":false"
                + "}"
                + "],"
                + "\"refundQuantity\":0,"
                + "\"quantity\":1,"
                + "\"integral\":0,"
                + "\"skuPrice\":39800"
                + "}"
                + "]";
        List<ShipmentItem> list = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(info,
                JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class,ShipmentItem.class));
//        System.out.println(list.get(0));
        RefundItem refundItem = new RefundItem();
        BeanMapper.copy(list.get(0), refundItem);
        System.out.println(refundItem.getAttrs());
    }

    @Test
    public void testreceiveJitShipmentResult() {

        Map<String, Object> params = Maps.newTreeMap();

        params.put("appKey", "pousheng");
        params.put("pampasCall", "jit.shipments.api");


        YyEdiShipInfo yyEdiShipInfo = new YyEdiShipInfo();

        yyEdiShipInfo.setWeight(100L);
        yyEdiShipInfo.setShipmentCorpCode("STO");
        yyEdiShipInfo.setYyEDIShipmentId("7777777");
        yyEdiShipInfo.setShipmentSerialNo("yyedei777777");
        yyEdiShipInfo.setShipmentDate(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(new DateTime()));
        yyEdiShipInfo.setShipmentId("SHP102744"); //shipmentcode
        ArrayList<YyEdiShipInfo> yyEdiShipInfos = Lists.newArrayList(yyEdiShipInfo);

        String data = mapper.toJson(yyEdiShipInfos);
        log.info("data:{}", data);
        params.put("shipInfo", data);
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
            .putString(toVerify, UTF_8)
            .putString("6a0e@93204aefe45d47f6e488", UTF_8).hash().toString();
        params.put("sign", sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

        post("http://127.0.0.1:8092/api/gateway", params);

    }

    @Test
    public void testSyncPostageOrder(){
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey","pousheng");
        params.put("pampasCall","push.out.open.order.api");
        List<OpenFullOrderInfo> orderInfos = Lists.newArrayList();
        OpenFullOrderInfo openFullOrderInfo = new OpenFullOrderInfo();

        OpenFullOrder openFullOrder = new OpenFullOrder();
        openFullOrder.setBuyerName("wuwen");
        openFullOrder.setChannel("official");
        openFullOrder.setCompanyCode("300");
        openFullOrder.setCreatedAt("2018112323302");
        openFullOrder.setFee(1000L);
        openFullOrder.setOriginFee(1000L);
        openFullOrder.setShipFee(0L);
        openFullOrder.setOutOrderId("wuwen-3367839+121");
        openFullOrder.setOriginShipFee(0L);
        openFullOrder.setShipmentType(1);
        openFullOrder.setPayType(1);
        openFullOrder.setShopCode("13000997");
        openFullOrder.setStatus(1);
        openFullOrder.setStockId("300-13000997");

        // 50个子订单
        List<OpenFullOrderItem> items =  Lists.newArrayList();
        //        for (int i = 0; i < 1; i++) {
        OpenFullOrderItem item = new OpenFullOrderItem();
        item.setOutSkuorderId("236703963+111_677778");
        item.setSkuCode("BYC0001$");
        item.setItemType("01");
        item.setItemName("邮费商品");
        item.setQuantity(10);
        item.setOriginFee(100L);
        item.setDiscount(0L);
        item.setCleanPrice(100L);
        item.setCleanFee(100L);
        items.add(item);
        //        }

        OpenFullOrderAddress address = new OpenFullOrderAddress();
        address.setProvince("江苏省");
        address.setCity("南京市");
        address.setRegion("江宁区");
        address.setDetail("胜利路89号");
        address.setMobile("18021529596");
        address.setPhone("02512345678");
        address.setReceiveUserName("悟问");

        openFullOrderInfo.setOrder(openFullOrder);
        openFullOrderInfo.setItem(items);
        openFullOrderInfo.setAddress(address);
        orderInfos.add(openFullOrderInfo);
        String data = JsonMapper.nonEmptyMapper().toJson(orderInfos);
        log.info("order",data);
        params.put("orderInfo",data);
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher()
            .putString(toVerify, Charsets.UTF_8)
            .putString("6a0e@93204aefe45d47f6e488", Charsets.UTF_8).hash().toString();
        params.put("sign",sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));

        post("http://127.0.0.1:8092/api/gateway",params);

        //post("http://middle-api-test.pousheng.com/api/gateway",params);
        //        post("http://middle-api-prepub.pousheng.com/api/gateway",params);
    }

}
