package com.pousheng.middle.open.ych;


import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.open.ych.token.YchToken;
import com.pousheng.middle.open.ych.utils.YchSignUtils;
import io.terminus.common.exception.JsonResponseException;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

@Slf4j
public class YchClient {

    private final YchToken token;

    public YchClient(YchToken token) {
        this.token = token;
    }

    public String post(String path, TreeMap<String, String> params) {
        params.put("time", String.valueOf(new Date().getTime()));
        params.put("appKey", token.getAppKey());

        StringBuilder query = new StringBuilder();
        for (Entry<String, String> en : params.entrySet()) {
            query.append(en.getKey());
            query.append("=");
            query.append(en.getValue());
            query.append("&");
        }

        String sign = YchSignUtils.getSignature(token.getSecret(), params);
        query.append("sign=");
        query.append(sign);
        return doPost(normalize(path), query.toString());
    }

    private String normalize(String path) {
        if (path.startsWith("/")) {
            return token.getGateway() + path;
        }
        return token.getGateway() + "/" + path;
    }

    private String doPost(String url, String content) {
        HttpRequest r = HttpRequest
                .post(url)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8)
                .send(content);
        if (r.ok()) {
            return r.body();
        } else {
            log.error("fail to request ych,url={},content={},cause:{}", url, content, r.body());
            throw new JsonResponseException("ych.request.fail");
        }
    }
}