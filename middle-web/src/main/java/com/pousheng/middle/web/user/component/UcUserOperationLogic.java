package com.pousheng.middle.web.user.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.auth.dto.LoginTokenInfo;
import com.pousheng.auth.dto.UcUserInfo;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 用户中心用户操作逻辑
 * Created by songrenfei on 2017/6/23
 */
@Slf4j
@Component
public class UcUserOperationLogic {


    @Value("${gateway.member.host}")
    private String userCenterGateway;


    /**
     * 获取用户登录token
     * @param userName 用户名
     * @param password 密码
     * @return 用户登录token信息
     */
    public LoginTokenInfo getUserToken(String userName, String password){
        Map<String, String> params = Maps.newHashMap();


        String url = userCenterGateway+"/oauth/token";

        params.put("client_id","c3");
        params.put("client_secret","secret");
        params.put("grant_type", "password");
        params.put("connection", "basic_password");
        params.put("username", userName);
        params.put("password", password);

        log.info("[USER-CENTER] start get user center token,url:{} params:{}",url,params);

        String resultJson = HttpRequest.post(url)
                //.basic("c3", "secret")
                .contentType("application/x-www-form-urlencoded")
                .connectTimeout(1000000)
                .readTimeout(1000000)
                .form(params)
                .body();

        log.info("[USER-CENTER] get user center token result:{} ",resultJson);

        return JsonMapper.nonDefaultMapper().fromJson(resultJson,LoginTokenInfo.class);


    }

    public UcUserInfo authGetUserInfo(String token){

        String resultJson = HttpRequest.get(userCenterGateway+"/userinfo")
                .authorization("Bearer " + token)
                .connectTimeout(1000000)
                .readTimeout(1000000)
                .body();

        return JsonMapper.nonDefaultMapper().fromJson(resultJson,UcUserInfo.class);

    }


    public Response<UcUserInfo> createUcUser(String name, String password){
        try {

            UserInfoWithPassword up = new UserInfoWithPassword();
            up.setPassword(password);
            up.setUsername(name);
            Map<String, Object> metadata = Maps.newHashMap();
            metadata.put("rolesJson","[\"OPERATOR\"]");
            up.setType(3);
            up.setMetadata(metadata);
            String userInfoJson = JsonMapper.nonDefaultMapper().toJson(up);

            String result = HttpRequest.post(userCenterGateway+"/v1/users").contentType("application/json").send(userInfoJson).body();
            log.info("create uc user name:{} result:{}",name,result);
            checkResult(result);
            return Response.ok(JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(result,UcUserInfo.class));

        }catch (ServiceException e){
            log.error("create uc user name:{} fail,error:{}",name, e.getMessage());
            return Response.fail(e.getMessage());

        }catch (Exception e){
            log.error("create uc user name:{} fail,cause:{}",name, Throwables.getStackTraceAsString(e));
            return Response.fail("create.uc.user.fail");

        }

    }


    public Response<UcUserInfo> updateUcUser(Long userId,String name, String password){

        try {

            UserInfoWithPassword up = new UserInfoWithPassword();
            if(!Strings.isNullOrEmpty(password)){
                up.setPassword(password);
            }
            up.setUsername(name);
            String userInfoJson = JsonMapper.nonDefaultMapper().toJson(up);

            String result = HttpRequest.put(userCenterGateway+"/v1/users/"+userId).contentType("application/json").send(userInfoJson).body();
            log.info("update uc user name:{} result:{}",name,result);
            //checkResult(result);
            return Response.ok(JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(result,UcUserInfo.class));

        }catch (ServiceException e){
            log.error("update uc user name:{} fail,error:{}",name, e.getMessage());
            return Response.fail(e.getMessage());

        }catch (Exception e){
            log.error("create uc user name:{} fail,cause:{}",name, Throwables.getStackTraceAsString(e));
            return Response.fail("update.uc.user.fail");

        }

    }



    public Response<List<UcUserInfo>> queryUserByName(String userName){
        try {
            String criteria =  "/v1/users?query=username:"+userName;
            String result = HttpRequest.get(userCenterGateway+criteria).connectTimeout(1000000).readTimeout(1000000).body();
            log.info("query uc user result:{}",result);
           // checkResult(result);
            return Response.ok(JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(result,JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class,UcUserInfo.class)));
        }catch (ServiceException e){
            log.error("query user center user by name:{} fail,error:{}",userName,e.getMessage());
            return Response.fail(e.getMessage());
        }catch (Exception e){
            log.error("query user center user by name:{} fail,cause:{}",userName,Throwables.getStackTraceAsString(e));
            return Response.fail("query.user.center.user.fail");
        }


    }

    private void checkResult(String resultJson){
        Map<String,String> resultMap = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(resultJson,
                JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(Map.class,String.class,String.class));

        if(resultMap.containsKey("error")){
            log.error("invoking user center fail,error:{}",resultMap.get("error"));
            throw new ServiceException("invoking.user.center.fail");
        }
    }


    @Data
    private static class UserInfoWithPassword extends UcUserInfo {

        private String password;
        private Integer type;
    }


}
