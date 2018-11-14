package com.pousheng.middle.web.biz.controller;

import com.github.kevinsawicki.http.HttpRequest;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * biz操作客户端
 * Created by songrenfei on 2018/11/14
 */
@Component
@Slf4j
public class BizOperationClient {


    private final String host;

    public static final int HttpTime = 300000;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    @Autowired
    public BizOperationClient(@Value("${middle.api.gateway}") String host) {
        this.host = host;
    }


    /**
     * 发送put请求
     * @param path
     * @param params
     * @return
     */
    public Response<Boolean> put(String path, Map<String, Object> params){
        log.info("request to middle api {} with params: {}", host + "/" + path, params);
        HttpRequest r = HttpRequest.put(host+ "/" + path, params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8).connectTimeout(HttpTime).readTimeout(HttpTime);
        String body = r.body();
        if (r.ok()){
            log.info("request to middle api {} with params: {} success response body:{}", host + "/" + path, params,body);
            Boolean isSuccess = Boolean.parseBoolean(body);
            if (isSuccess){
                return Response.ok();
            } else {
                return Response.fail("update.biz.fail");
            }

        } else{
            log.info("request to middle api {} with params: {} fail response body:{}", host + "/" + path, params,body);
            return Response.fail("request.middle.fail");
        }
    }

}
