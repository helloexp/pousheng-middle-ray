package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelShipmentBody;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelShipmentInfo;
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
public class SycYYEdiOrderCancelApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    /**
     *
     * @param reqeustData
     * @return
     */
    public String doCancelOrder(List<YYEdiCancelShipmentInfo> reqeustData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        YYEdiCancelShipmentBody body = new YYEdiCancelShipmentBody();
        body.setRequestData(reqeustData);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(body);
        log.info("paramJson:{}",paramJson);
        String gateway = hkGateway+"/commonerp/erp/sal/updateordercancelstatus";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("result:{}",responseBody);
        return responseBody;
    }
}
