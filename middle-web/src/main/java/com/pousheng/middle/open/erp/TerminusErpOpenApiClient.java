package com.pousheng.middle.open.erp;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.terminus.open.client.common.OpenClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.open.erp
 * 2018/10/11 11:33
 * pousheng-middle
 */
@Component
@Slf4j
public class TerminusErpOpenApiClient {
    @Value("${terminus.erp.app.key:pousheng}")
    private String erpAppKey;

    @Value("${terminus.erp.secret:6a0e@93204aefe45d47f6e488}")
    private String erpSecret;

    @Value("${terminus.erp.gateway:http://retail-gateway-pagoda-prod.app.terminus.io/api/gateway}")

    private String erpGateWay;

    public String doPost(String method, Map<String, Object> params) {
        Map<String, Object> context = Maps.newTreeMap();
        context.putAll(params);
        context.put("appKey", erpAppKey);
        context.put("pampasCall", method);
        context.put("sign", generateSign(context));

        HttpRequest request = HttpRequest.post(erpGateWay).form(context, "UTF-8");
        final String resp = request.body();
        if (request.ok()) {
            return resp;
        } else {
            log.error("fail to do post to erp,method={},params={},cause:{}",
                    method, params, resp);
            throw new OpenClientException(500, resp);
        }

    }

    private String generateSign(Map<String, Object> params) {
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        return Hashing.md5().newHasher()
                .putString(toVerify, Charsets.UTF_8)
                .putString(erpSecret, Charsets.UTF_8)
                .hash()
                .toString();
    }


}
