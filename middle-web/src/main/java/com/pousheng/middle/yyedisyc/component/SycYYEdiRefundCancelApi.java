package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelBody;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelRequest;
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
public class SycYYEdiRefundCancelApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;

    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;

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
        log.info("yyedi-cancel-refund paramJson:{}, serialNo{}",paramJson,serialNo);
        String gateway = hkGateway+"/common/yyedi/default/getcancelrefund";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync cancel refund to yyedi result:{}, serialNo:{}",responseBody,serialNo);
        return responseBody;
    }
}
