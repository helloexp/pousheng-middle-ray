package com.pousheng.middle.open.ych.logger;


import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import com.pousheng.middle.open.ych.token.YchToken;
import com.pousheng.middle.open.ych.utils.YchSignUtils;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

@Slf4j
public class LogSender {

    private final YchToken token;

    public LogSender(YchToken token) {
        this.token = token;
    }

    public void send(String path, TreeMap<String, String> params) {
        params.put("time", String.valueOf(new Date().getTime()));
        params.put("appKey", token.getAppKey());

        StringBuilder reqContent = new StringBuilder();
        for (Entry<String, String> en : params.entrySet()) {
            reqContent.append(en.getKey());
            reqContent.append("=");
            reqContent.append(en.getValue());
            reqContent.append("&");
        }

        String url = normalize(path);
        try {
            String sign = YchSignUtils.getSignature(token.getSecret(), params);
            reqContent.append("sign=");
            reqContent.append(sign);

            String response = doPost(url, reqContent.toString());
            LogResponse logResponse = JsonMapper.nonDefaultMapper().fromJson(response, LogResponse.class);
            if (!logResponse.isSuccess()) {
                log.error("fail to send log to url:{},params:{},cause:{}",
                        url, params, logResponse.getErrMsg());
            }
        } catch (Exception e) {
            log.error("fail to send log to url:{},params:{},cause:{}",
                    url, params, Throwables.getStackTraceAsString(e));

        }

    }

    private String normalize(String path) {
        if (path.startsWith("/")) {
            return token.getGatewayOfLog() + path;
        }
        return token.getGatewayOfLog() + "/" + path;
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
            log.error("fail to post log to url={},content={},cause:{}", url, content, r.body());
            throw new JsonResponseException("post.log.fail");
        }
    }
}