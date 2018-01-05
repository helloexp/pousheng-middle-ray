package com.pousheng.middle.web.user.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.sql.Date;
import java.util.Map;

/**
 * 会员中心简易client
 * Created by cp on 7/18/17.
 */
@Component
@Slf4j
public class MemberCenterClient {

    @Value("${gateway.member.host}")
    private String gateway;

    public String doGet(String path, Map<String, String> params) {
        log.info("path = {}, params = {}", path, params);

        HttpRequest r = HttpRequest
                .get(normalizeGateway(gateway) + normalizePath(path), params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            return r.body();
        } else {
            try {
                String error = r.body();
                log.error("fail to do get request to member center,path={},params={},cause:{}", path, params, error);
                if (!Strings.isNullOrEmpty(error)) {
                    ErrorData errorData = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(error, ErrorData.class);
                    throw new JsonResponseException(errorData.getMessage());
                }
                throw new JsonResponseException("member.center.request.fail");
            }catch (Exception e) {
                log.error("[get]parse json error, cause: {}", Throwables.getStackTraceAsString(e));
                throw new JsonResponseException("member.center.request.fail");
            }
        }
    }

    public String doPost(String path, Map<String, String> params) {
        HttpRequest r = HttpRequest
                .post(normalizeGateway(gateway) + normalizePath(path), params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            return r.body();
        } else {
            log.error("fail to do post request to member center,path={},params={},cause:{}",
                    path, params, r.body());
            throw new JsonResponseException("member.center.request.fail");
        }
    }

    public String doPostJson(String path, Map<String, Object> params) {
        String paramData = JsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(params);
        log.info("path = {}, paramJsonData = {}", path, paramData);

        HttpRequest r = HttpRequest
                .post(normalizeGateway(gateway) + normalizePath(path))
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8)
                .contentType(HttpRequest.CONTENT_TYPE_JSON)
                .send(paramData);
        if (r.ok()) {
            return r.body();
        } else {
            try {
                String error = r.body();
                log.error("fail to do post json request to member center, path = {}, params = {}" +
                        ", cause : {}", path, params, error);
                if (!Strings.isNullOrEmpty(error)) {
                    ErrorData errorData = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(error, ErrorData.class);
                    throw new JsonResponseException(errorData.getMessage());
                }
                throw new JsonResponseException("member.center.request.fail");
            }catch (Exception e) {
                log.error("[post]parse json error, cause: {}", Throwables.getStackTraceAsString(e));
                throw new JsonResponseException("member.center.request.fail");
            }
        }
    }

    private String normalizeGateway(String gateway) {
        if (gateway.endsWith("/")) {
            return gateway.substring(0, gateway.length() - 1);
        }
        return gateway;
    }

    private String normalizePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        return "/" + path;
    }

    /**
     * eg:
     * "timestamp": 1508497725031,
     * "status": 500,
     * "error": "Internal Server Error",
     * "exception": "org.springframework.http.converter.HttpMessageNotReadableException",
     * "message": "系统异常，请检查网络！",
     * "path": "/api/api123"
     */
    @Data
    public static class ErrorData implements Serializable {
        private static final long serialVersionUID = 4973333335753706812L;
        private Date timestamp;
        private Integer status;
        private String error;
        private String exception;
        private String message;
        private String path;
    }

}
