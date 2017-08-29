package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.dto.HkRequestHead;
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

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycHkShipmentOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    public String doSyncShipmentOrder( List<SycHkShipmentOrderDto> orders){

       String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);


        HkRequestHead head = HkRequestHead.create()
                .appCode("ec").format("json").isSign("1").method("addOrder")
                .sendTime(DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .signBody("09D13CCA2BE96F99BFC24DA0E691DE67").version("1.0").serialNo(serialNo);

        SycHkShipmentOrderBody orderBody = new SycHkShipmentOrderBody();
        orderBody.setOrders(orders);

        /*SycHkShipmentOrderForm orderForm = new SycHkShipmentOrderForm();
        orderForm.setHead(head);
        orderForm.setBody(orderBody);*/


        String paramJson = JsonMapper.nonEmptyMapper().toJson(orderBody);
        log.info("paramJson:{}",paramJson);
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

        log.info("result:{}",responseBody);
        return responseBody;
    }
}
