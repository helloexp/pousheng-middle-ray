package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefund;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundItem;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiReturnInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiReturnInfoBody;
import com.pousheng.middle.yyedisyc.dto.trade.YYEditReturnInfoRequest;
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
        log.info("paramJson:{}",paramJson);
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

        log.info("result:{}",responseBody);
        return responseBody;
    }
}
