package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.dto.HkRequestHead;
import com.pousheng.middle.hksyc.dto.trade.SycHkShipmentOrderBody;
import com.pousheng.middle.hksyc.dto.trade.SycHkShipmentOrderDto;
import com.pousheng.middle.hksyc.utils.Numbers;
import io.swagger.models.auth.In;
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
public class SycHkOrderCancelApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    /**
     * 取消订单
     * @param shopCode 恒康店铺内码id
     * @param orderId 发货单号或退货单号，根据type
     * @param type 0:发货单  1:退货单
     */
    public void doCancelOrder(String shopCode, Long orderId, Integer type){
        //0  取消  1 删除
        Integer operationType = 0;

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        Map<String, Object> params = Maps.newHashMap();
        params.put("shopid",shopCode);
        params.put("orderNo",orderId);
        params.put("optype",operationType);
        params.put("type",type);


        String paramJson = JsonMapper.nonEmptyMapper().toJson(params);
        log.debug("paramJson:{}",paramJson);
        String hkGateway ="http://esbt.pousheng.com/commonerp/erp/sal/updateordercancelstatus";
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
