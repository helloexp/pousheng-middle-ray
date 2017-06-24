/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.interceptors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.auth.model.User;
import com.pousheng.auth.service.UserReadService;
import com.pousheng.middle.constants.Constants;
import com.pousheng.middle.utils.ParanaUserMaker;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
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

    private LoadingCache<Long, Response<User>> userCache;

    @Autowired
    private UserReadService userReadService;

    @PostConstruct
    public void init() {
        userCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build(new CacheLoader<Long, Response<User>>() {
            @Override
            public Response<User> load(Long userId) throws Exception {
                return userReadService.findById(userId);
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
                Response<? extends User> result = userCache.getUnchecked(userId);
                if (!result.isSuccess()) {
                    log.warn("failed to find user where id={},error code:{}", userId, result.getError());
                    return false;
                }
                User user = result.getResult();
                if (user != null) {
                 /*   if (Objects.equal(user.getStatus(), UserStatus.DELETED.value()) ||
                            Objects.equal(user.getStatus(), UserStatus.FROZEN.value()) ||
                            Objects.equal(user.getStatus(), UserStatus.LOCKED.value())) {
                        session.invalidate();
                        return false;
                    }*/
                   /* if (!userTypeBean.isAdmin(user) && !userTypeBean.isOperator(user)) {
                        log.warn("user(id={})'s is not admin or operator, its type is {}", userId, user.getType());
                        session.invalidate();
                        return false;
                    }
                    if (userTypeBean.isOperator(user)) {
                        Operator operator = RespHelper.or500(operatorReadService.findByUserId(user.getId()));
                        if (operator == null || !Objects.equal(operator.getStatus(), 1)) {
                            session.invalidate();
                            return false;
                        }
                    }*/
                    ParanaUser paranaUser = ParanaUserMaker.from(user);
                    UserUtil.putCurrentUser(paranaUser);
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
