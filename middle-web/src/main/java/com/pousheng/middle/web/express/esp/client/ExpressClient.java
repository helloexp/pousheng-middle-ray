package com.pousheng.middle.web.express.esp.client;

import com.alibaba.fastjson.JSONObject;
import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.web.express.esp.bean.ESPExpressCodeSendResponse;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/27
 */
@Service
@Slf4j
public class ExpressClient {

    static final String getExpressNoPath = "common/pserp/tp/getexpressno";
    static final String sendExpressNoPath = "common/pserp/wms/oxoexpressno";
    static final String sendStoreExpressNoPath = "common/pserp/tp/getexpressno";
    static final String cancelExpressNoPath = "common/pserp/tp/cancelorder";

    @Value("${gateway.esb.host}")
    private String esbHost;

    @Value("${gateway.esb.accessKey}")
    private String accessKey;

    /**
     * 获取快递单号接口
     *
     * @param jsonParam
     * @return
     */
    public JSONObject getExpressNo(String jsonParam) {
        String jsonResponse = postJsonCustom(getExpressNoPath, jsonParam);
        if (StringUtils.hasText(jsonResponse)) {
            JSONObject espExpressResponses = JSONObject.parseObject(jsonResponse);
            return espExpressResponses;
        }
        return null;
    }

    /**
     * 仓发接收OXO运单号接口
     *
     * @param jsonParam
     * @return
     */
    public ESPExpressCodeSendResponse sendExpressCode(String jsonParam) {
        String jsonResponse = postJsonCustom(sendExpressNoPath, jsonParam);
        if (StringUtils.hasText(jsonResponse)) {
            ESPExpressCodeSendResponse espExpressCodeSendResponse = JSONObject.parseObject(jsonResponse, ESPExpressCodeSendResponse.class);
            return espExpressCodeSendResponse;
        }
        return null;
    }

    /**
     * 仓发接收OXO运单号接口
     *
     * @param jsonParam
     * @return
     */
    public ESPExpressCodeSendResponse cancelExpressCode(String jsonParam) {
        String jsonResponse = postJsonCustom(cancelExpressNoPath, jsonParam);
        if (StringUtils.hasText(jsonResponse)) {
            ESPExpressCodeSendResponse espExpressCodeSendResponse = JSONObject.parseObject(jsonResponse, ESPExpressCodeSendResponse.class);
            return espExpressCodeSendResponse;
        }
        return null;
    }

    /**
     * 店发接收OXO运单号接口
     *
     * @param jsonParam
     * @return
     */
    public JSONObject sendStoreExpressCode(String jsonParam) {
        String jsonResponse = postJsonCustom(sendStoreExpressNoPath, jsonParam);
        if (StringUtils.hasText(jsonResponse)) {
            JSONObject espExpressCodeSendResponse = JSONObject.parseObject(jsonResponse);
            return espExpressCodeSendResponse;
        }
        return null;
    }


    public String postJsonCustom(String path, String json) {
        log.info("request to {} with params: {}", esbHost + "/" + path, json);

        HttpRequest r = HttpRequest.post(esbHost + "/" + path)
                .header("verifycode", accessKey)
                .contentType("application/json")
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8)
                .send(json);

        if (r.ok()) {
            String body = r.body();
            log.debug("call esb success,request return body:{}", body);
            return body;
        } else {
            int code = r.code();
            String body = r.body();
            log.error("failed to post (path={}, params:{}), http code:{}, message:{}",
                    path, json, code, body);
            throw new ServiceException("erp.request.post.fail");
        }
    }
}
