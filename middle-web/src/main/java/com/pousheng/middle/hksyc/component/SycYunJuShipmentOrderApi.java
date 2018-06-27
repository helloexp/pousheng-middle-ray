package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.dto.HkRequestHead;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.hksyc.dto.YJSyncShipmentRequest;
import com.pousheng.middle.hksyc.dto.trade.SycHkShipmentOrderBody;
import com.pousheng.middle.hksyc.dto.trade.SycHkShipmentOrderDto;
import com.pousheng.middle.hksyc.dto.trade.SycShipmentOrderResponse;
import com.pousheng.middle.hksyc.utils.Numbers;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Throwables;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYunJuShipmentOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    public YJRespone doSyncShipmentOrder(YJSyncShipmentRequest syncShipmentRequest) {

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

        String paramJson = JsonMapper.nonEmptyMapper().toJson(syncShipmentRequest);
        log.info("paramJson:{}", paramJson);
        String uri = "/common-yjerp/yjerp/default/pushmgorderout";
        String gateway = hkGateway + uri;
        String responseBody = null;
        try {
            responseBody = HttpRequest.post(gateway)
                    .header("verifycode", accessKey)
                    .header("serialNo", serialNo)
                    .header("sendTime", DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                    .contentType("application/json")
                    .send(paramJson)
                    .connectTimeout(1000000).readTimeout(1000000)
                    .body();

            log.info("rpc yunju mgorderout  responseBody={}", responseBody);

        } catch (Exception e) {
            log.error("rpc yunju mgorderout exception happens,exception={}", Throwables.getStackTrace(e));
            return  null;
        }
        YJRespone response = JsonMapper.nonEmptyMapper().fromJson(responseBody, YJRespone.class);

        return response;
    }
}
