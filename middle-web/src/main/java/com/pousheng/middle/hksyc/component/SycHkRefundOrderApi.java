package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefund;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundDto;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundItem;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundOrderBody;
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

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    public String doSyncRefundOrder(SycHkRefund sycHkRefund, List<SycHkRefundItem> sycHkRefundItems){

       String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

       /* Map<String, Object> params = Maps.newHashMap();
        params.put("nonce",Numbers.getNonce());
        params.put("timestamp","1234567890");
        params.put("tradeRefund",sycHkRefund);
        params.put("tradeRefundItems",sycHkRefundItems);*/

        SycHkRefundOrderBody refundOrderBody = new SycHkRefundOrderBody();
        SycHkRefundDto sycHkRefundDto = new SycHkRefundDto();
        sycHkRefundDto.setTradeRefund(sycHkRefund);
        sycHkRefundDto.setTradeRefundItems(sycHkRefundItems);
        refundOrderBody.setRefundorder(sycHkRefundDto);

        String paramJson = JsonMapper.nonEmptyMapper().toJson(refundOrderBody);
        log.info("sync refund code:{} to hk paramJson:{}",sycHkRefund.getRefundNo(),paramJson);
        //String gateway =hkGateway+"/commonerp/erp/sal/addrefund";
        String gateway =hkGateway+"/common-terminus/skx-oms/default/getrefundordersreceive";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync refund code:{} to hk result:{}",sycHkRefund.getRefundNo(),responseBody);
        return responseBody;
    }
}
