package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.utils.Numbers;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 查询恒康仓或门店商品库存信息
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycHkOrderCancelApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    /**
     * 取消订单
     * @param shopCode 恒康店铺内码id
     * @param orderId 发货单号或退货单号，根据type
     * @param type 0:发货单  1:退货单
     * @param operationType 0 取消,1删除
     */
    public String doCancelOrder(String shopCode, Long orderId, Integer operationType,Integer type){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        Map<String, Object> params = Maps.newHashMap();
        if(Objects.equal(0,operationType)){
            params.put("orderno",orderId);
        }else {
            params.put("refundno",orderId);
        }
        params.put("nonce",Numbers.getNonce());
        params.put("timestamp",String.valueOf(System.currentTimeMillis()));


        String paramJson = JsonMapper.nonEmptyMapper().toJson(params);
        log.info("paramJson:{}",paramJson);
        //String gateway = hkGateway+"/commonerp/erp/sal/updateordercancelstatus";
        String gateway = "";
        if(Objects.equal(0,operationType)){
            gateway = hkGateway+"/common-terminus/skx-oms/default/getcancelorder";
        }else {
            gateway = hkGateway+"//common-terminus/skx-oms/default/getcancelrefundorder";
        }
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
