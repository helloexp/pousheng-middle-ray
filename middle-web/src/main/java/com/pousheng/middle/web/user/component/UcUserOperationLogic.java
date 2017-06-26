package com.pousheng.middle.web.user.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.pousheng.auth.dto.LoginTokenInfo;
import com.pousheng.auth.dto.UcUserInfo;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 用户中心用户操作逻辑
 * Created by songrenfei on 2017/6/23
 */
@Slf4j
@Component
public class UcUserOperationLogic {

    private Map<String, String> params = Maps.newHashMap();

    /**
     * 获取用户登录token
     * @param userName 用户名
     * @param password 密码
     * @return 用户登录token信息
     */
    public LoginTokenInfo getUserToken(String userName, String password){
        String url = "http://devt-account.pousheng.com/oauth/token";

        params.put("client_id","c3");
        params.put("client_secret","secret");
        params.put("grant_type", "password");
        params.put("connection", "basic_password");
        params.put("username", userName);
        params.put("password", password);

        String resultJson = HttpRequest.post(url)
                //.basic("c3", "secret")
                .contentType("application/x-www-form-urlencoded")
                .connectTimeout(1000000)
                .readTimeout(1000000)
                .form(params)
                .body();

        return JsonMapper.nonDefaultMapper().fromJson(resultJson,LoginTokenInfo.class);


    }

    public UcUserInfo authGetUserInfo(String token){

        String resultJson = HttpRequest.get("http://devt-account.pousheng.com/userinfo")
                .authorization("Bearer " + token)
                .connectTimeout(1000000)
                .readTimeout(1000000)
                .body();

        return JsonMapper.nonDefaultMapper().fromJson(resultJson,UcUserInfo.class);

    }
}
