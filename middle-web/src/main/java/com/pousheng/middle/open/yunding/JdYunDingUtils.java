package com.pousheng.middle.open.yunding;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.terminus.common.exception.ServiceException;
import io.terminus.open.client.common.OpenClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author bernie
 * @date 2019/6/3
 */
@Component
@Slf4j
public class JdYunDingUtils {

    @Value("${jd.yunding.app.key}")
    private String appKey;
    @Value("${jd.yunding.secret}")
    private String secret;
    @Value("${jd.yunding.gateway}")
    private String gateway;

    /**
     * @param shopId
     * @param method
     * @param requestParams
     * @return
     */
    public String post(Long shopId, String method, Map<String, Object> requestParams) {

        Map<String, Object> params = this.handleRequestParams(appKey, secret, method, requestParams);
        return this.post(gateway, params);
    }

    public String post(String url, Map<String, Object> params) {
        HttpRequest request = HttpRequest.post(url).connectTimeout(10000).readTimeout(10000).form(params);
        if (!request.ok()) {
            log.error("post request url:{} params:{} fail,response result:{}", new Object[]{url, params, request.body()});
            throw new ServiceException("request.fail");
        } else {
            String result = request.body();
            log.debug("post request url:{} result:{}", url, result);
            return result;
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
}
