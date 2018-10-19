package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.*;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYYEdiRefundOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;

    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;

    public String doSyncRefundOrder(List<YYEdiReturnInfo> requestData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

        YYEdiReturnInfoBody body = new YYEdiReturnInfoBody();
        body.setRequestData(requestData);
        YYEditReturnInfoRequest request = new YYEditReturnInfoRequest();
        request.setBody(body);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(request);
        log.info("sync refund to yyedi erp paramJson:{}, serialNo:{}",paramJson,serialNo);
        String gateway =hkGateway+"/common/yyedi/default/pushrefunds";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync refund to yyedi erp result:{}, serialNo:{}",responseBody,serialNo);
        return responseBody;
    }


    public String doSyncYJErpRefundOrder(List<YJErpRefundInfo> requestData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        log.info("sync refund to yj erp paramJson:{} serialNo:{}",paramJson,serialNo);
        String gateway =hkGateway + "/common-yjerp/yjerp/default/pushmgorderexchangeset";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync refund to yj erp result:{}, serialNo:{}",responseBody,serialNo);
        return responseBody;
    }
}
