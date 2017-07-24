package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.dto.HkRequestHead;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefund;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundItem;
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
public class SycHkRefundOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    public void doSyncRefundOrder(SycHkRefund sycHkRefund, List<SycHkRefundItem> sycHkRefundItems){

       String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

        Map<String, Object> params = Maps.newHashMap();
        params.put("tradeRefund",sycHkRefund);
        params.put("tradeRefundItems",sycHkRefundItems);

        String paramJson = JsonMapper.nonEmptyMapper().toJson(params);
        log.debug("paramJson:{}",paramJson);
        String hkGateway ="http://esbt.pousheng.com/commonerp/erp/sal/addrefund";
        String responseBody = HttpRequest.post(hkGateway)
                .header("verifycode","646edef40c9c481fb9cd9c61a41dabc1")
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("result:{}",responseBody);

    }
}
