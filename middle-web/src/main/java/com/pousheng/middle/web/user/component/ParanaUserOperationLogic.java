package com.pousheng.middle.web.user.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * parana用户操作逻辑
 * Created by songrenfei on 2017/6/23
 */
@Slf4j
@Component
public class ParanaUserOperationLogic {


    @Value("${gateway.parana.host}")
    private String paranaGateway;


    public Response<Boolean> updateUserStatus(Integer status,Long id){

        try {
            Map<String,Object> params = Maps.newHashMap();
            params.put("ids[]",id);
            params.put("status",status);
            String result = HttpRequest.post(paranaGateway+"/api/user/statuses").connectTimeout(1000000).readTimeout(1000000).form(params).body();
            log.info("update user ids:{}  status:{} result:{}",id,status,result);
            return Response.ok();
        }catch (Exception e){
            log.info("update user ids:{}  status:{} cause:{}",id,status,Throwables.getStackTraceAsString(e));
            return Response.fail("update.user.status.fail");

        }
    }

}
