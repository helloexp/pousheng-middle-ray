package com.pousheng.middle.open.ych;


import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import com.pousheng.middle.open.ych.token.YchToken;
import io.terminus.common.exception.JsonResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

@Component
@Slf4j
public class YchClient {

    private final YchToken token;

    @Autowired
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

        String sign = getSignature(token.getSecret(), params);
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

    private String getSignature(String appSecret, Map<String, String> params) {
        try {
            StringBuilder combineString = new StringBuilder();
            combineString.append(appSecret);
            for (Entry<String, String> entry : params.entrySet()) {
                combineString.append(entry.getKey() + entry.getValue());
            }
            combineString.append(appSecret);

            byte[] bytesOfMessage = combineString.toString().getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            return bytesToHexString(md.digest(bytesOfMessage));
        } catch (Exception e) {
            log.error("fail to generate signature for params:{},cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

    private String bytesToHexString(byte[] src) {
        try {
            if (src == null || src.length <= 0) {
                return null;
            }

            StringBuilder stringBuilder = new StringBuilder("");
            for (int i = 0; i < src.length; i++) {
                int v = src[i] & 0xFF;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }
                stringBuilder.append(hv);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("fail to transfer bytes:{} to hex string,cause:{}",
                    src, Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }
}