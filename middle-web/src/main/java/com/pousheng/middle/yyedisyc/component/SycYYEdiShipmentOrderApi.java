package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.dto.HkRequestHead;
import com.pousheng.middle.hksyc.dto.trade.SycHkShipmentOrderBody;
import com.pousheng.middle.hksyc.dto.trade.SycHkShipmentOrderDto;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiShipmentInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiShipmentInfoBody;
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
public class SycYYEdiShipmentOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    public String doSyncShipmentOrder( List<YYEdiShipmentInfo> requestData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

        YYEdiShipmentInfoBody body = new YYEdiShipmentInfoBody();
        body.setRequestData(requestData);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(body);
        log.info("yyedi.paramJson:{}",paramJson);
        String gateway =hkGateway + "/commonerp/erp/sal/addorder";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync yyedi result:{}",responseBody);
        return responseBody;
    }
}
