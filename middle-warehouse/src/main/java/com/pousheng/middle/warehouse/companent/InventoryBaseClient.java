package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 对接库存中心的RESTAPI封装
 *
 * Author: feisheng.ch
 * Date: 2018-05-23
 */
@Component
@Slf4j
public class InventoryBaseClient {

    private final String host;

    public static final int HttpTime = 3000;

    @Autowired
    public InventoryBaseClient(@Value("${gateway.inventory.host}") String host) {
        this.host = host;
    }

    public String get(String path, Integer pageNo, Integer pageSize, Map<String, Object> params) {
        if (pageNo != null) {
            params.put("pageNo", pageNo.toString());
        }
        if (pageSize != null) {
            params.put("pageSize", pageSize.toString());
        }

        log.info("start request to {} with params: {}", host + "/" + path, params);
        HttpRequest r = HttpRequest.get(host + "/" + path, params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            log.info("request success!");
            return r.body();
        } else {
            handleInvalidResponse(r.method(), r.code(), r.body(), path, JSON.toJSONString(params));
            // 上面已经肯定抛出异常
            return null;
        }
    }

    public Object get(String path, Integer pageNo, Integer pageSize,
                      Map<String, Object> params, Class clazz, boolean isList) {
        if (pageNo != null) {
            params.put("pageNo", pageNo.toString());
        }
        if (pageSize != null) {
            params.put("pageSize", pageSize.toString());
        }

        log.info("start request to {} with params: {}", host + "/" + path, params);
        HttpRequest r = HttpRequest.get(host + "/" + path, params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            log.info("request success!");
            return handleNormalResponse(path, params, r.body(), clazz, isList);
        } else {
            handleInvalidResponse(r.method(), r.code(), r.body(), path, JSON.toJSONString(params));
            // 上面已经肯定抛出异常
            return null;
        }
    }

    /**
     * 发送post请求
     * @param path
     * @param params
     * @param clazz
     * @return
     */
    public Object post(String path, Map<String, Object> params, Class clazz){
        log.info("request to {} with params: {}", host + "/" + path, params);
        HttpRequest r = null;
        try {
             r = HttpRequest.post(host+"/"+path, params, true)
                    .acceptJson()
                    .acceptCharset(HttpRequest.CHARSET_UTF8)
                    .connectTimeout(HttpTime)
                    .readTimeout(HttpTime);
        } catch (HttpRequest.HttpRequestException e) {
            // 捕获超时异常 库存中心响应超时
            log.error("call stock api time out");
            throw new ServiceException("inventory.response.timeout");
        }
        if(r.ok()){
            log.info("request success!");
            return handleNormalResponse(path, params, r.body(), clazz, false);
        }else{
            handleInvalidResponse(r.method(), r.code(), r.body(), path, JSON.toJSONString(params));
            // 上面已经肯定抛出异常
            return null;
        }
    }

    /**
     * 发送put请求
     * @param path
     * @param params
     * @param clazz
     * @return
     */
    public Object put(String path, Map<String, Object> params, Class clazz){
        log.info("request to {} with params: {}", host + "/" + path, params);
        HttpRequest r = HttpRequest.put(host+"/"+path, params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if(r.ok()){
            log.info("request success!");
            return handleNormalResponse(path, params, r.body(), clazz, false);
        }else{
            handleInvalidResponse(r.method(), r.code(), r.body(), path, JSON.toJSONString(params));
            // 上面已经肯定抛出异常
            return null;
        }
    }

    /**
     * 发送delete请求
     * @param path
     * @param params
     * @param clazz
     * @return
     */
    public Object delete(String path, Map<String, Object> params, Class clazz){
        log.info("request to {} with params: {}", host + "/" + path, params);
        HttpRequest r = HttpRequest.delete(host+"/"+path, params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if(r.ok()){
            log.info("request success!");
            return handleNormalResponse(path, params, r.body(), clazz, false);
        }else{
            handleInvalidResponse(r.method(), r.code(), r.body(), path, JSON.toJSONString(params));
            // 上面已经肯定抛出异常
            return null;
        }
    }

    /**
     * post数据
     * @param path
     * @param json
     * @return
     */
    public Object postJson(String path, String json, Class clazz){
        log.info("request to {} with params: {}", host + "/" + path, json);

        HttpRequest r = HttpRequest.post(host+"/"+path)
                .contentType("application/json")
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8)
                .send(json);

        if(r.ok()){
            return handleNormalResponse(path, null, r.body(), clazz, false);
        }else{
            handleInvalidResponse(r.method(), r.code(), r.body(), path, json);
            // 上面已经肯定抛出异常
            return null;
        }
    }

    /**
     * put数据
     * @param path
     * @param json
     * @return
     */
    public Object putJson(String path, String json, Class clazz){
        log.info("request to {} with params: {}", host + "/" + path, json);

        HttpRequest r = HttpRequest.put(host+"/"+path)
                .contentType("application/json")
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8)
                .send(json);

        if(r.ok()){
            return handleNormalResponse(path, null, r.body(), clazz, false);
        }else{
            handleInvalidResponse(r.method(), r.code(), r.body(), path, json);
            // 上面已经肯定抛出异常
            return null;
        }
    }

    private void handleInvalidResponse (String method, int code, String body, String reqPath, String params) {
        log.error("failed to {} (path={}, params:{}), http code:{}, message:{}", method,
                reqPath, params, code, body);
        handleJsonResponseException(body);
        throw new ServiceException("inventory.request.unknown.response");
    }

    private Object handleNormalResponse(String path, Map<String, Object> params, String body, Class clazz, boolean isList) {
        try {
            if (isList) {
                return JSON.parseArray(body, clazz);
            }

            return JSON.parseObject(body, clazz);

        } catch (Exception e) {
            log.error("failed to request url when parse response body (path={}, params:{}, body:{}), cause:{}",
                    path, params, body, Throwables.getStackTraceAsString(e));
            throw new ServiceException(e);
        }
    }

    private static void handleJsonResponseException (String body) {
        String retErrorCode = null;

        try {
            if (body.contains("JsonResponseException")) {
                JSONObject retObj = JSON.parseObject(body);
                if (retObj.containsKey("message")) {
                    String message = retObj.getString("message");
                    if (StringUtils.isNotBlank(message)) {
                        if (-1 != message.indexOf("JsonResponseException: ")) {
                            retErrorCode = message.substring(
                                    message.indexOf("JsonResponseException: ")+"JsonResponseException: ".length());
                        } else {
                            retErrorCode = message;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("error to parse body to handle json response exception: {}", Throwables.getStackTraceAsString(e));
        }

        if (StringUtils.isNotBlank(retErrorCode)) {
            throw new ServiceException(retErrorCode);
        }
    }

    public static void main (String[] args) {
        handleJsonResponseException("{\"timestamp\":1527837279229,\"status\":500,\"error\":\"Internal Server Error\",\"exception\":\"io.terminus.common.exception.JsonResponseException\",\"message\":\"shop.stock.rule.not.find\",\"path\":\"/api/inventory/shop-rule/findByShopIdAndSku/260/884292330249\"}");
    }

}
