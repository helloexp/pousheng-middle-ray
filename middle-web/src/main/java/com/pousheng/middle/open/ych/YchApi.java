package com.pousheng.middle.open.ych;

import com.google.common.base.Throwables;
import com.pousheng.middle.open.ych.response.ComputeRiskResponse;
import com.pousheng.middle.open.ych.response.VerifyPassedResponse;
import com.pousheng.middle.open.ych.response.VerifyUrlGetResponse;
import com.pousheng.middle.open.ych.response.YchResponse;
import com.pousheng.middle.open.ych.token.TaobaoToken;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.TreeMap;

/**
 * Created by cp on 9/15/17.
 */
@Slf4j
public class YchApi {

    private final YchClient client;

    private final TaobaoToken taobaoToken;

    public YchApi(YchClient client,
                  TaobaoToken taobaoToken) {
        this.client = client;
        this.taobaoToken = taobaoToken;
    }

    public Response<Boolean> sendLoginLog(TreeMap<String, String> params) {
        try {
            params.put("topAppKey", taobaoToken.getAppKey());
            params.put("appName", taobaoToken.getAppName());
            String body = client.post("/login", params);
            YchResponse response = JsonMapper.nonDefaultMapper().fromJson(body, YchResponse.class);
            if (!response.isSuccess()) {
                return Response.fail(response.getErrMsg());
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("fail to send login log with params:{},cause:{}", params, Throwables.getStackTraceAsString(e));
            return Response.fail("login.log.send.fail");
        }
    }

    public Response<Double> computeRisk(TreeMap<String, String> params) {
        try {
            params.put("appId", taobaoToken.getAppKey());
            params.put("appName", taobaoToken.getAppName());
            String body = client.post("/computeRisk", params);
            ComputeRiskResponse response = JsonMapper.nonDefaultMapper().fromJson(body, ComputeRiskResponse.class);
            if (!response.isSuccess()) {
                return Response.fail(response.getErrMsg());
            }
            return Response.ok(response.getRisk());
        } catch (Exception e) {
            log.error("fail to compute risk with params:{},cause:{}", params, Throwables.getStackTraceAsString(e));
            return Response.fail("compute.risk.fail");
        }
    }

    public Response<String> getVerifyUrl(TreeMap<String, String> params) {
        try {
            params.put("appId", taobaoToken.getAppKey());
            params.put("appName", taobaoToken.getAppName());
            String body = client.post("/getVerifyUrl", params);
            VerifyUrlGetResponse response = JsonMapper.nonDefaultMapper().fromJson(body, VerifyUrlGetResponse.class);
            if (!response.isSuccess()) {
                return Response.fail(response.getErrMsg());
            }
            return Response.ok(response.getVerifyUrl());
        } catch (Exception e) {
            log.error("fail to get verify url with params:{},cause:{}", params, Throwables.getStackTraceAsString(e));
            return Response.fail("verify.url.get.fail");
        }
    }

    public Response<Boolean> isVerifyPassed(TreeMap<String, String> params) {
        try {
            String body = client.post("/isVerifyPassed", params);
            VerifyPassedResponse response = JsonMapper.nonDefaultMapper().fromJson(body, VerifyPassedResponse.class);
            if (!response.isSuccess()) {
                return Response.fail(response.getErrMsg());
            }
            return Response.ok("success".equals(response.getVerifyResult()));
        } catch (Exception e) {
            log.error("fail to verify if passed with params:{},cause:{}", params, Throwables.getStackTraceAsString(e));
            return Response.fail("verify.passed.fail");
        }
    }

}
