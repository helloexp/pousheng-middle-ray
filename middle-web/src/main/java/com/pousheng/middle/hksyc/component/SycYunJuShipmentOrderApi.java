package com.pousheng.middle.hksyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.hksyc.dto.YJSyncCancelRequest;
import com.pousheng.middle.hksyc.dto.YJSyncShipmentRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import io.terminus.common.exception.JsonResponseException;
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
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYunJuShipmentOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private String yjGateway;
    private String yjAccessKey;
    @Autowired
    OpenShopCacher openShopCacher;

    public YJRespone doSyncShipmentOrder(YJSyncShipmentRequest syncShipmentRequest,Long shopId) {
        OpenShop openshop = openShopCacher.findById(shopId);
        this.yjGateway = openshop.getGateway();
        this.yjAccessKey = openshop.getAccessToken();
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(syncShipmentRequest);
        log.info("doSyncShipmentOrder to yj out order id:{} paramJson:{}", syncShipmentRequest.getOrder_sn(), paramJson);
        String uri = "/pushmgorderout";
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

            log.info("rpc yunju mgorderout out order id:{}  responseBody={}", syncShipmentRequest.getOrder_sn(), responseBody);

        } catch (Exception e) {
            log.error("rpc yunju mgorderout out order id:{} exception happens,exception={}", syncShipmentRequest.getOrder_sn(), Throwables.getStackTrace(e));
            return null;
        }
        YJRespone response = JsonMapper.nonEmptyMapper().fromJson(responseBody, YJRespone.class);

        return response;
    }


    public void doSyncCancelResult(YJSyncCancelRequest yjSyncCancelRequest,Long shopId) {
        OpenShop openshop = openShopCacher.findById(shopId);
        this.yjGateway = openshop.getGateway();
        this.yjAccessKey = openshop.getAccessToken();
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String uri = "/domgordercancelreturn";
        String paramJson = JsonMapper.nonEmptyMapper().toJson(yjSyncCancelRequest);
        log.info("doSyncShipmentOrder to yj out order id:{} paramJson:{}", yjSyncCancelRequest.getOrder_sn(), paramJson);

        String gateway = yjGateway + uri;
        String responseBody;

        responseBody = HttpRequest.post(gateway)
                .header("verifycode", yjAccessKey)
                .header("serialNo", serialNo)
                .header("sendTime", DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .send(paramJson)
                .connectTimeout(1000000).readTimeout(1000000)
                .body();
        YJRespone response = JsonMapper.nonEmptyMapper().fromJson(responseBody, YJRespone.class);
        log.info("rpc yunju mg order cancel return  order id:{}  responseBody={}", yjSyncCancelRequest.getOrder_sn(), responseBody);
        if(response.getError()!=0){
            throw new JsonResponseException(response.getError_info());
        }
      }
}
