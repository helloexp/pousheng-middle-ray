package com.pousheng.middle.open.api;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.pousheng.middle.order.dto.reverseLogistic.*;
import com.pousheng.middle.web.order.component.ReverseExpressInfoLogic;
import com.pousheng.middle.web.order.component.ReverseHeadlessInfoLogic;
import com.pousheng.middle.web.order.component.ReverseInstoreInfoLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author bernie
 * @date 2019/6/3 逆物流信息接入
 */
@OpenBean
@Slf4j
public class WmsOpenApi {

    @Autowired
    private ReverseExpressInfoLogic reverseExpressInfoLogic;
    @Autowired
    private ReverseInstoreInfoLogic reverseInstoreInfoLogic;
    @Autowired
    private ReverseHeadlessInfoLogic reverseHeadlessInfoLogic;

    @OpenMethod(key = "wms.sync.reverse.express", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public List<String> syncExpress(@NotNull(message = "reverse.express.is.null") String data) {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("sync-reverse.express-start param: data [{}] ", data);
        List<ReverseExpressInfoDto> items = JsonMapper.nonEmptyMapper().fromJson(data,
            JsonMapper.nonEmptyMapper().createCollectionType(List.class, ReverseExpressInfoDto.class));
        List<String> failList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(items)) {
            Response<Boolean> response = reverseExpressInfoLogic.batchCreateOrUpdate(items);
            if (!response.isSuccess()) {
                for (ReverseExpressInfoDto expressInfoDto : items) {
                    failList.add(expressInfoDto.getTransferOrderId() + "," + expressInfoDto.getLineNo());
                }
            }
        }
        stopwatch.stop();
        log.info("sync-reverse.express.elapsed={}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return failList;
    }

    @OpenMethod(key = "wms.sync.reverse.headless", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public List<String> syncErpHandleResult(@NotNull(message = "reverse.headless.is.null") String data) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("sync-reverse.headless-start param: data [{}] ", data);
        List<ReverseHeadlessDto> items = JsonMapper.nonEmptyMapper().fromJson(data,
            JsonMapper.nonEmptyMapper().createCollectionType(List.class, ReverseHeadlessDto.class));
        List<String> failList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(items)) {
            Response<Boolean> response = reverseHeadlessInfoLogic.batchCreateOrUpdate(items);
            if (!response.isSuccess()) {
                for (ReverseHeadlessDto reverseHeadlessDto : items) {
                    failList.add(reverseHeadlessDto.getUniqueNo());
                }
            }
        }
        stopwatch.stop();
        log.info("sync-reverse.headless.elapsed={}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return failList;
    }

    @OpenMethod(key = "wms.sync.reverse.instore", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public List<String> syncInstore(@NotNull(message = "reverse.instore.is.null") String data) {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("sync-reverse.instore-start param: data [{}] ", data);
        List<ReverseInstoreDto> items = JsonMapper.nonEmptyMapper().fromJson(data,
            JsonMapper.nonEmptyMapper().createCollectionType(List.class, ReverseInstoreDto.class));
        List<String> failList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(items)) {
            Response<Boolean> response = reverseInstoreInfoLogic.batchCreateOrUpdate(items);

            if (!response.isSuccess()) {
                for (ReverseInstoreDto expressInfoDto : items) {
                    failList.add(expressInfoDto.getInstoreNo() + "," + expressInfoDto.getInstoreDetailNo());
                }
            }
        }
        stopwatch.stop();
        log.info("sync-reverse.instore.elapsed={}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return failList;
    }

    //public static void main(String[] args) {
    //
    //    //ReverseExpressInfoDto expressInfoDto=buildExpressDto(0);
    //    List<ReverseInstoreDto> reverseHeadlessDtos = Lists.newArrayList();
    //    for(int i=0;i<4;i++){
    //        reverseHeadlessDtos.add(buildInstoreDto(i));
    //        //reverseHeadlessDtos.add(buildHeadLessDto(i*2));
    //    }
    //
    //
    //    Map<String, Object> param = handleParam(JsonMapper.nonEmptyMapper().toJson(reverseHeadlessDtos));
    //    System.out.println(JsonMapper.nonEmptyMapper().toJson(param));
    //    System.out.println(JsonMapper.nonEmptyMapper().toJson(reverseHeadlessDtos));
    //    //List<String> stringList = Lists.newArrayList("入库单001+明细001", "入库单001+明细002");
    //    //System.out.println(JsonMapper.nonEmptyMapper().toJson(stringList));
    //
    //}

    public static ReverseLogisticTime getTime() {
        ReverseLogisticTime reverseLogisticTime = new ReverseLogisticTime();
        reverseLogisticTime.setCreatedAt(new Date());
        reverseLogisticTime.setCloseAt(new Date());
        reverseLogisticTime.setAsnCreateAt(new Date());
        reverseLogisticTime.setAccomplishAt(new Date());
        reverseLogisticTime.setArriveWmsDate(new Date());
        reverseLogisticTime.setConfirmReceiveDate(new Date());
        reverseLogisticTime.setResponseDate(new Date());
        reverseLogisticTime.setInFlowAt(new Date());
        return reverseLogisticTime;
    }

    public static WaybillItemDto getWayBillItem() {
        WaybillItemDto waybillItemDto = new WaybillItemDto();
        waybillItemDto.setBrand("品牌");
        waybillItemDto.setInstoreQuantity(2);
        waybillItemDto.setQuantity(2);
        waybillItemDto.setGoodsNo("货号001");
        waybillItemDto.setSkuNo("Sku");
        waybillItemDto.setSize("尺码大小");
        waybillItemDto.setLeftSize("左脚尺寸");
        waybillItemDto.setRightSize("右脚尺寸");
        waybillItemDto.setMaterialInfo("实物信息");
        waybillItemDto.setShoeboxInfo("鞋盒信息");
        waybillItemDto.setWeight("重量");

        return waybillItemDto;
    }

    public static ReverseHeadlessDto buildHeadLessDto(int i) {
        ReverseHeadlessDto reverseHeadlessDto = new ReverseHeadlessDto();
        reverseHeadlessDto.setHeadlessNo("无头件单号001_"+i);
        reverseHeadlessDto.setChannel("vipoxo_"+i);
        reverseHeadlessDto.setShop("唯品会旗舰店_"+i);
        reverseHeadlessDto.setExpressNo("物流单号001_"+i);
        reverseHeadlessDto.setCustomer("客户名称_"+i);
        reverseHeadlessDto.setInventoryProfitNo("盘盈入库单号001_"+i);
        reverseHeadlessDto.setPhone("1539989892_"+i);

        reverseHeadlessDto.setPlatformNo("平台单号0034_"+i);

        reverseHeadlessDto.setReason("七天无理由_"+i);

        reverseHeadlessDto.setRelateAsn("关联ASN_"+i);
        reverseHeadlessDto.setShipCompany("出货公司_"+i);
        reverseHeadlessDto.setShipMode("出货方式_2");
        reverseHeadlessDto.setShipExpressNo("出货单号_"+i);
        reverseHeadlessDto.setUniqueNo("00"+i);
        reverseHeadlessDto.setStatus("关闭");

        reverseHeadlessDto.setWaybillItemDto(getWayBillItem());
        reverseHeadlessDto.setTimeInfo(getTime());
        return reverseHeadlessDto;
    }

    public static ReverseInstoreDto buildInstoreDto(int i) {
        int ext=i+1000;
        ReverseInstoreDto instoreDto = new ReverseInstoreDto();
        instoreDto.setChannel("vipoxo"+ ext);
        instoreDto.setShop("唯品会旗舰店"+ext);
        instoreDto.setStatus("已接收");
        instoreDto.setCarrierExpressNo("承运商运单号00"+ext);
        instoreDto.setPlatformNo("平台销售单单号"+ext);
        instoreDto.setErpNo("erp单号"+ext);
        instoreDto.setRealExpressNo("实际运单号"+ext);
        instoreDto.setRtxAnomalyBig("异常大类"+ext);
        instoreDto.setRtxAnomalySmall("异常小类"+ext);
        instoreDto.setInstoreDetailNo("退货入库单号行号"+i);
        instoreDto.setInstoreNo("退货入款单号"+i);
        instoreDto.setWaybillItemDto(getWayBillItem());
        instoreDto.setTimeInfo(getTime());
        instoreDto.setCreatedBy("操作人"+ext);
        instoreDto.setCustomerNote("备注"+ext);

        return instoreDto;
    }

    public static ReverseExpressInfoDto buildExpressDto(int i) {
        int ext=i+30;
        ReverseExpressInfoDto expressInfoDto = new ReverseExpressInfoDto();
        WaybillDto waybillDto = new WaybillDto();
        waybillDto.setCarrierCode("SF"+ext);
        waybillDto.setCarrierName("顺丰速递"+ext);
        waybillDto.setBuyerMemo("买家备注"+ext);
        waybillDto.setExpressNo("物流单号"+ext);
        waybillDto.setFee(10000L);
        waybillDto.setPayer("王先生"+ext);
        waybillDto.setPaidAfterDelivery(0);
        waybillDto.setShipper("发货方"+ext);

        SenderDto senderDto = new SenderDto();
        senderDto.setCity("上海市"+ext);
        senderDto.setProvince("上海"+ext);
        senderDto.setRegion("普陀区"+ext);
        senderDto.setDetail("西凉路222号"+ext);
        senderDto.setSenderName("王刚"+ ext);
        senderDto.setSenderMobile("1531998898"+ext);
        waybillDto.setSenderInfo(senderDto);
        expressInfoDto.setWaybillDto(waybillDto);
        expressInfoDto.setHasOrder(0);
        expressInfoDto.setInstoreNo("89898989入库单号"+ext);
        expressInfoDto.setTransferOrderId("交接单号000"+i);
        expressInfoDto.setLineNo("行号00"+i);
        expressInfoDto.setStatus("0");
        expressInfoDto.setChannel("vipoxo"+ ext);
        expressInfoDto.setShop("唯品会旗舰店"+ext);
        expressInfoDto.setTimeInfo(getTime());
        expressInfoDto.setCreatedBy("创建人"+ext);
        return expressInfoDto;
    }

    public static Map<String, Object> handleParam(String data) {
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", "pousheng");
        params.put("data", data);
        params.put("pampasCall", "wms.sync.reverse.instore");

        String sign = sign("6a0e@93204aefe45d47f6e488", params);
        params.put("sign", sign);
        return params;

    }

    public static String sign(String secret, Map<String, Object> params) {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
            return Hashing.md5().newHasher().putString(toVerify, Charsets.UTF_8).putString(secret, Charsets.UTF_8)
                .hash().toString();
        } catch (Exception var4) {
            log.error("call parana open api sign fail, params:{},cause:{}", params,
                Throwables.getStackTraceAsString(var4));
            throw new OpenClientException(500, "parana.sign.fail");
        }
    }

}
