package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.dto.JitOrderReceiptRequest;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.hksyc.utils.Numbers;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Throwables;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 发送JIT 订单回执接口
 * @author tanlongjun
 */
@Component
@Slf4j
public class JitOrderReceiptApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 成功
     */
    public static final int SUCCESS=1;

    /**
     * 失败
     */
    public static final int FAILED=2;

    private String yjGateway;
    private String yjAccessKey;
    @Autowired
    OpenShopCacher openShopCacher;

    /**
     * 发送回执
     * @param receiptRequest
     * @return
     */
    public YJRespone sendReceipt(JitOrderReceiptRequest receiptRequest, Long shopId) {

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        OpenShop openshop = openShopCacher.findById(shopId);
        this.yjGateway = openshop.getGateway();
        this.yjAccessKey = openshop.getAccessToken();
        String paramJson = JsonMapper.nonEmptyMapper().toJson(receiptRequest);
        log.info("sendReceipt to jit out order id:{} paramJson:{}", receiptRequest.getOrder_sn(),paramJson);
        String uri = "/pushmgorderdealreturn";
        String gateway = yjGateway + uri;
        String responseBody = null;
        try {
            responseBody = HttpRequest.post(gateway)
                    .header("verifycode", yjAccessKey)
                    .header("serialNo", serialNo)
                    .header("sendTime", DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                    .contentType("application/json")
                    .send(paramJson)
                    .connectTimeout(1000000).readTimeout(1000000)
                    .body();

            log.info("request jit pushmgorderdealreturn out order id:{}  responseBody={}",receiptRequest.getOrder_sn(), responseBody);

        } catch (Exception e) {
            log.error("request jit pushmgorderdealreturn out order id:{} exception happens,exception={}",receiptRequest.getOrder_sn(), Throwables.getStackTrace(e));
            return  null;
        }
        YJRespone response = JsonMapper.nonEmptyMapper().fromJson(responseBody, YJRespone.class);

        return response;
    }
}
