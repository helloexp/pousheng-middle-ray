/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.interceptors;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.auth.model.MiddleUser;
import com.pousheng.auth.service.PsUserReadService;
import com.pousheng.middle.constants.Constants;
import com.pousheng.middle.utils.ParanaUserMaker;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.auth.model.Operator;
import io.terminus.parana.auth.service.OperatorReadService;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.Op;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

/**
 * Author:  songrenfei
 * Date: 2017-06-28
 */
@Slf4j
@Component
public class LoginInterceptor extends HandlerInterceptorAdapter {

    private LoadingCache<Long, Response<Optional<MiddleUser>>> userCache;

    @Autowired
    private PsUserReadService userReadService;
    @RpcConsumer
    private OperatorReadService operatorReadService;

    @PostConstruct
    public void init() {
        userCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build(new CacheLoader<Long, Response<Optional<MiddleUser>>>() {
            @Override
            public Response<Optional<MiddleUser>> load(Long userId) throws Exception {
                return userReadService.findByOutId(userId);
            }
        });
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object userIdInSession = session.getAttribute(Constants.SESSION_USER_ID);
            if (userIdInSession != null) {

                final Long userId = Long.valueOf(userIdInSession.toString());
                Response<Optional<MiddleUser>> resultRes = userCache.getUnchecked(userId);
                if (!resultRes.isSuccess()) {
                    log.warn("failed to find middleUser where id={},error code:{}", userId, resultRes.getError());
                    return false;
                }
                Optional<MiddleUser> middleUserOptional = resultRes.getResult();
                if (middleUserOptional.isPresent()) {
                    MiddleUser middleUser = middleUserOptional.get();
                    if (!Objects.equal(middleUser.getType(), UserType.ADMIN.value()) && !Objects.equal(middleUser.getType(), UserType.OPERATOR.value())) {
                        log.warn("middleUser(id={})'s is not admin or operator, its type is {}", userId, middleUser.getType());
                        session.invalidate();
                        return false;
                    }
                    if (Objects.equal(middleUser.getType(), UserType.OPERATOR.value())) {
                        Operator operator = RespHelper.or500(operatorReadService.findByUserId(middleUser.getId()));
                        if (operator == null || !Objects.equal(operator.getStatus(), 1)) {
                            session.invalidate();
                            return false;
                        }
                    }
                    ParanaUser paranaUser = ParanaUserMaker.from(middleUser);
                    UserUtil.putCurrentUser(paranaUser);
                }else {
                    log.error("not find middle user by out id:{}",userId);
                    return Boolean.FALSE;
                }
            }
        }
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserUtil.clearCurrentUser();
    }
}
