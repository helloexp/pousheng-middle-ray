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
public class SycYYEdiOrderCancelApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;

    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;

    @Value("${gateway.yjerp.host}")
    private String yjGateway;

    @Value("${gateway.yjerp.accessKey}")
    private String yjaccessKey;

    /**
     *
     * @param reqeustData
     * @return
     */
    public String doCancelOrder(List<YYEdiCancelInfo> reqeustData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        YYEdiCancelBody body = new YYEdiCancelBody();
        body.setRequestData(reqeustData);
        YYEdiCancelRequest request = new YYEdiCancelRequest();
        request.setBody(body);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(request);
        log.info("start do cancel yyedi order paramJson:{}",paramJson);
        String gateway = hkGateway+"/common/yyedi/default/cancelorder";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("end do cancel yyedi order paramJson:{},result:{}",paramJson,responseBody);
        return responseBody;
    }

    public String doYJErpCancelOrder(List<YJErpCancelInfo> requestData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        log.info("sync cancel shipment to yj erp paramJson :{}",paramJson);
        String gateway = yjGateway + "/common-yjerp/yjerp/default/pushmgordercancel";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",yjaccessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync cancel shipment to yj erp result:{}",responseBody);
        return responseBody;
    }
}
