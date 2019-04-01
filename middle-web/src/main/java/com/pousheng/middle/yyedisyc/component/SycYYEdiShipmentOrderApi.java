package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.YYEdiResponse;
import com.pousheng.middle.yyedisyc.dto.trade.*;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYYEdiShipmentOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;

    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;

    private static final String SID = "PS_ERP_WMS_bcorders";

    public String doSyncShipmentOrder(YYEdiShipmentInfo requestData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        YYEdiShipmentInfoBody body = new YYEdiShipmentInfoBody();
        body.bizContent(requestData).sid(SID).tranReqDate(DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)));
        String paramJson = JsonMapper.nonEmptyMapper().toJson(body);
        log.info("sync shipment to yyedi erp paramJson:{},serialNo:{}", paramJson, serialNo);
        String gateway = hkGateway + "/common/pserp/wms/pushbcorders";
        String responseBody = HttpRequest.post(gateway)
                .contentType("application/json")
                .header("verifycode", accessKey)
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("sync shipment to yyedi erp result:{},serialNo:{}", responseBody, serialNo);
        return responseBody;
    }


    public String doSyncYJErpShipmentOrder(List<YJErpShipmentInfo> requestData) {
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        log.info("sync shipment to yj erp paramJson:{}, serialNo:{}",paramJson,serialNo);
        String gateway =hkGateway + "/common-yjerp/yjerp/default/pushmgorderset";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync shipment to yj erp result:{} ,serialNo:{}",responseBody,serialNo);
        return responseBody;
    }
}
