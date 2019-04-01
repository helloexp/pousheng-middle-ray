package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.dto.HkRequestHead;
import com.pousheng.middle.hksyc.dto.trade.SycHkShipmentOrderBody;
import com.pousheng.middle.hksyc.dto.trade.SycHkShipmentOrderDto;
import com.pousheng.middle.hksyc.utils.Numbers;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycHkShipmentOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    public String doSyncShipmentOrder( List<SycHkShipmentOrderDto> orders,String shipmentCode){

       String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);


        HkRequestHead head = HkRequestHead.create()
                .appCode("ec").format("json").isSign("1").method("addOrder")
                .sendTime(DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .signBody("09D13CCA2BE96F99BFC24DA0E691DE67").version("1.0").serialNo(serialNo);

        SycHkShipmentOrderBody orderBody = new SycHkShipmentOrderBody();
        orderBody.setOrders(orders);

        /*SycHkShipmentOrderForm orderForm = new SycHkShipmentOrderForm();
        orderForm.setHead(head);
        orderForm.setBody(orderBody);*/


        String paramJson = JsonMapper.nonEmptyMapper().toJson(orderBody);
        log.info("sync shipment code:{} to hk  paramJson:{}",shipmentCode,paramJson);
        //String gateway =hkGateway + "/commonerp/erp/sal/addorder";
        String gateway =hkGateway + "/common-terminus/skx-oms/default/getordersreceive";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

         log.info("sync shipment code:{} to hk result:{}",shipmentCode,responseBody);
        return responseBody;
    }


    /**
     * skx解挂
     * @param refundOrderNo
     * @return
     */
    public String syncUnfreezeShipment(String refundOrderNo){
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        Map<String, Object> params = Maps.newHashMap();
        params.put("refundorderno",refundOrderNo);
        params.put("nonce",Numbers.getNonce());
        params.put("timestamp",String.valueOf(System.currentTimeMillis()));

        String paramJson = JsonMapper.nonEmptyMapper().toJson(params);
        log.info("sync skx unfreeze shipment id:{} to hk paramJson:{}",refundOrderNo,paramJson);

        String gateway = hkGateway+"/common-terminus/skx-oms/default/doorderunlock";
        //skx强调要用get请求，唉。。不规范。
        String responseBody = HttpRequest.get(gateway,params,true)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                //.contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .acceptJson()
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync skx unfreeze shipment id:{} to hk result:{}",refundOrderNo,responseBody);
        return responseBody;
    }
}
