package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.dto.YJExchangeReturnRequest;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefund;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundItem;
import com.pousheng.middle.hksyc.utils.Numbers;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Throwables;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYunjuRefundOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    public YJRespone doSyncRefundOrder(YJExchangeReturnRequest returnRequest){

       String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);



        String paramJson = JsonMapper.nonEmptyMapper().toJson(returnRequest);
        log.info("rpc yunju mgexchangereturn exchange_id:{} paramJson:{}",returnRequest.getExchange_id(),paramJson);
        String uri="/common-yjerp/yjerp/default/pushmgexchangereturn"; // FIXME: 2018/4/12
        String gateway =hkGateway+uri;
        String responseBody="";
        try {
            responseBody= HttpRequest.post(gateway)
                    .header("verifycode",accessKey)
                    .header("serialNo",serialNo)
                    .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                    .contentType("application/json")
                    .send(paramJson)
                    .connectTimeout(10000).readTimeout(10000)
                    .body();

            log.info("rpc yunju mgexchangereturn exchange_id:{} responseBody={}",returnRequest.getExchange_id(),responseBody);
        }catch ( Exception e){

            log.error("rpc yunju mgexchangereturn exchange_id:{} exception happens,exception={}",returnRequest.getExchange_id(), Throwables.getStackTrace(e));
            return null;
        }

        YJRespone response  = JsonMapper.nonEmptyMapper().fromJson(responseBody,YJRespone.class);

        return response;
    }
}
