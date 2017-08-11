package com.pousheng.erp.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@Component
@Slf4j
public class ErpClient {

    public static final ObjectMapper mapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private final String host;

    private final String accessKey;

    @Autowired
    public ErpClient(@Value("${gateway.erp.host}") String host,
                     @Value("${gateway.erp.accessKey}") String accessKey) {
        this.host = host;
        this.accessKey = accessKey;
    }

    public String get(String path,
                      Date start,
                      Date end,
                      Integer pageNo,
                      Integer pageSize,
                      Map<String, String> params) {
        if (start != null) {
            params.put("start_datetime", formatter.print(start.getTime()));
        }
        if (end != null) {
            params.put("end_datetime", formatter.print(end.getTime()));
        }
        if (pageNo != null) {
            params.put("current_page", pageNo.toString());
        }
        if (pageSize != null) {
            params.put("page_size", pageSize.toString());
        }

        log.info("request to {} with params: {}", host + "/" + path, params);

        HttpRequest r = HttpRequest.get(host + "/" + path, params, true)
                .header("verifycode", accessKey)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            return handleResponse(path, params, r.body());
        } else {
            int code = r.code();
            String body = r.body();
            log.error("failed to get (path={}, params:{}), http code:{}, message:{}",
                    path, params, code, body);
            throw new ServiceException("erp.request.get.fail");
        }
    }


    public String post(String path, Map<String, String> params){
        log.info("request to {} with params: {}", host + "/" + path, params);
        HttpRequest r = HttpRequest.post(host+"/"+path, params, true)
                .header("verifycode", accessKey)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if(r.ok()){
            String body = r.body();
            try {
                JsonNode root = mapper.readTree(body);
                boolean success = root.findPath("retCode").asInt() == 0;
                if (!success) {
                    String errorCode = root.findPath("retMessage").textValue();
                    log.error("request url:{} failed,error code:{}", path, errorCode);
                    throw new ServiceException(errorCode);
                }
                return body;
            } catch (IOException e) {
                log.error("failed to request url (path={}, params:{}), cause:{}",
                        path, params, Throwables.getStackTraceAsString(e));
                throw new ServiceException(e);
            }
        }else{
            int code = r.code();
            String body = r.body();
            log.error("failed to post (path={}, params:{}), http code:{}, message:{}",
                    path, params, code, body);
            throw new ServiceException("erp.request.post.fail");
        }
    }

    public String postJson(String path, String json){
        log.info("request to {} with params: {}", host + "/" + path, json);

        HttpRequest r = HttpRequest.post(host+"/"+path)
                .header("verifycode", accessKey)
                .contentType("application/json")
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8)
                .send(json);

        if(r.ok()){
            String body = r.body();
            try {
                JsonNode root = mapper.readTree(body);
                boolean success = root.findPath("retCode").asInt() == 0;
                if (!success) {
                    String errorCode = root.findPath("retMessage").textValue();
                    log.error("request url:{} failed,error code:{}", path, errorCode);
                    throw new ServiceException(errorCode);
                }
                return body;
            } catch (IOException e) {
                log.error("failed to request url (path={}, params:{}), cause:{}",
                        path, json, Throwables.getStackTraceAsString(e));
                throw new ServiceException(e);
            }
        }else{
            int code = r.code();
            String body = r.body();
            log.error("failed to post (path={}, params:{}), http code:{}, message:{}",
                    path, json, code, body);
            throw new ServiceException("erp.request.post.fail");
        }
    }

    private String handleResponse(String path, Map<String, String> params, String body) {
        try {
            JsonNode root = mapper.readTree(body);
            boolean success = root.findPath("retCode").asInt() == 0;
            if (!success) {
                String errorCode = root.findPath("retMessage").textValue();
                log.error("request url:{} failed,error code:{}", path, errorCode);
                throw new ServiceException(errorCode);
            }
            return root.findPath("list").toString();
        } catch (IOException e) {
            log.error("failed to request url (path={}, params:{}), cause:{}",
                    path, params, Throwables.getStackTraceAsString(e));
            throw new ServiceException(e);
        }
    }
}
