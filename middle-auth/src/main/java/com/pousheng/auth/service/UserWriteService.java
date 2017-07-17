package com.pousheng.auth.service;

import com.google.common.base.Throwables;
import com.pousheng.auth.dao.UserDao;
import com.pousheng.auth.model.MiddleUser;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: 用户基本信息表写服务实现类
 * Date: 2017-06-23
 */
@Slf4j
@Service
public class UserWriteService {

    private final UserDao userDao;

    @Autowired
    public UserWriteService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Response<Long> create(MiddleUser middleUser) {
        try {
            userDao.create(middleUser);
            return Response.ok(middleUser.getId());
        } catch (Exception e) {
            log.error("create middleUser failed, middleUser:{}, cause:{}", middleUser, Throwables.getStackTraceAsString(e));
            return Response.fail("middleUser.create.fail");
        }
    }

    public Response<Boolean> update(MiddleUser middleUser) {
        try {
            return Response.ok(userDao.update(middleUser));
        } catch (Exception e) {
            log.error("update middleUser failed, middleUser:{}, cause:{}", middleUser, Throwables.getStackTraceAsString(e));
            return Response.fail("middleUser.update.fail");
        }
    }

    public Response<Boolean> deleteById(Long userId) {
        try {
            return Response.ok(userDao.delete(userId));
        } catch (Exception e) {
            log.error("delete user failed, userId:{}, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("user.delete.fail");
        }
    }
}
