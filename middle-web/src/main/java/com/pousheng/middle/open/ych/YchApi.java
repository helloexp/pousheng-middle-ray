package com.pousheng.middle.open.ych;

import com.pousheng.middle.open.ych.response.ComputeRiskResponse;
import com.pousheng.middle.open.ych.response.VerifyPassedResponse;
import com.pousheng.middle.open.ych.response.VerifyUrlGetResponse;
import com.pousheng.middle.open.ych.response.YchResponse;
import com.pousheng.middle.open.ych.token.TaobaoToken;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.TreeMap;

/**
 * Created by cp on 9/15/17.
 */
@Component
@Slf4j
public class YchApi {

    private final YchClient client;

    private final TaobaoToken taobaoToken;

    @Autowired
    public YchApi(YchClient client,
                  TaobaoToken taobaoToken) {
        this.client = client;
        this.taobaoToken = taobaoToken;
    }

    public void sendLoginLog(TreeMap<String, String> params) {
        params.put("topAppKey", taobaoToken.getAppKey());
        params.put("appName", "端点中台");
        String body = client.post("/login", params);
        YchResponse response = JsonMapper.nonDefaultMapper().fromJson(body, YchResponse.class);
        if (!response.isSuccess()) {
            log.error("fail to send login log with params:{},cause:{}", params, response.getErrMsg());
        }
    }

    public double computeRisk(TreeMap<String, String> params) {
        params.put("appId", taobaoToken.getAppKey());
        params.put("appName", "端点中台");
        String body = client.post("/computeRisk", params);
        ComputeRiskResponse response = JsonMapper.nonDefaultMapper().fromJson(body, ComputeRiskResponse.class);
        if (!response.isSuccess()) {
            log.error("fail to compute risk with params:{},cause:{}", params, response.getErrMsg());
            return 0;
        }
        return response.getRisk();
    }

    public String getVerifyUrl(TreeMap<String, String> params) {
        params.put("appId", taobaoToken.getAppKey());
        params.put("appName", "端点中台");
        String body = client.post("/getVerifyUrl", params);
        VerifyUrlGetResponse response = JsonMapper.nonDefaultMapper().fromJson(body, VerifyUrlGetResponse.class);
        if (!response.isSuccess()) {
            log.error("fail to get verify url with params:{},cause:{}", params, response.getErrMsg());
            return null;
        }
        return response.getVerifyUrl();
    }

    public boolean isVerifyPassed(TreeMap<String, String> params) {
        String body = client.post("/isVerifyPassed", params);
        VerifyPassedResponse response = JsonMapper.nonDefaultMapper().fromJson(body, VerifyPassedResponse.class);
        if (!response.isSuccess()) {
            log.error("fail to verify if passed with params:{},cause:{}", params, response.getErrMsg());
            return false;
        }
        return "success".equals(response.getVerifyResult());
    }

}
