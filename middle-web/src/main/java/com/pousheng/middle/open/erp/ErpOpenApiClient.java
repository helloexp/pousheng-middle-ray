package com.pousheng.middle.open.erp;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.terminus.open.client.common.OpenClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by cp on 8/23/17.
 */
@Component
@Slf4j
public class ErpOpenApiClient {

    private final ErpOpenApiToken token;

    @Autowired
    public ErpOpenApiClient(ErpOpenApiToken token) {
        this.token = token;
    }

    public String doPost(String method, Map<String, Object> params) {
        Map<String, Object> context = Maps.newTreeMap();
        context.putAll(params);
        context.put("appKey", token.getAppKey());
        context.put("pampasCall",method);
        context.put("sign", generateSign(context));

        HttpRequest request = HttpRequest.post(token.getGateway()).form(context, "UTF-8");
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
                .putString(token.getSecret(), Charsets.UTF_8)
                .hash()
                .toString();
    }


}
