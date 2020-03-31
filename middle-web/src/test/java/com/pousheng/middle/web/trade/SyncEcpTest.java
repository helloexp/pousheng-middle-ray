package com.pousheng.middle.web.trade;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.terminus.common.exception.ServiceException;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.order.dto.OpenClientOrderShipment;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/2
 * pousheng-middle
 */
@Slf4j
public class SyncEcpTest {

    @Test
    public void test(){
        OpenClientOrderShipment orderShipment = new OpenClientOrderShipment();
        orderShipment.setOuterOrderId("2988");
        orderShipment.setLogisticsCompany("shunfeng");
        //填写运单号
        orderShipment.setWaybill("123456");
        System.out.println(ship(orderShipment));
    }

    public String  ship(OpenClientOrderShipment openClientOrderShipment) {
        HashMap params = Maps.newHashMap();
        try {
            params.put("orderId", openClientOrderShipment.getOuterOrderId());
            params.put("shipmentCorpCode", openClientOrderShipment.getLogisticsCompany());
            params.put("shipmentSerialNo", openClientOrderShipment.getWaybill());

            Map<String, Object> params1 = this.handleRequestParams("yysport", "anywhere", "order.ship.api", params);
             return this.post("http://127.0.0.1:8001/api/gateway", params1);

        } catch (Exception var6) {
            log.error("ship param:{} fail,cause:{}", params, Throwables.getStackTraceAsString(var6));
            return "";
        }
    }



    private Map<String, Object> handleRequestParams(String appKey, String secret, String method, Map<String, Object> requestParams) {
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", appKey);
        params.put("pampasCall", method);
        params.putAll(requestParams);
        String sign = this.sign(secret, params);
        params.put("sign", sign);
        return params;
    }

    private String sign(String secret, Map<String, Object> params) {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
            return Hashing.md5().newHasher().putString(toVerify, Charsets.UTF_8).putString(secret, Charsets.UTF_8).hash().toString();
        } catch (Exception var4) {
            log.error("call parana open api sign fail, params:{},cause:{}", params, Throwables.getStackTraceAsString(var4));
            throw new OpenClientException(500, "parana.sign.fail");
        }
    }
    public String post(String url, Map<String, Object> params) {
        HttpRequest request = HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).form(params);
        if (!request.ok()) {
            log.error("post request url:{} params:{} fail,response result:{}", new Object[]{url, params, request.body()});
            throw new ServiceException("request.fail");
        } else {
            String result = request.body();
            log.debug("post request url:{} result:{}", url, result);
            return result;
        }
    }

}
