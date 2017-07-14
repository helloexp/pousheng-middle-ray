package com.pousheng.auth.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.auth.dao.UserDao;
import com.pousheng.auth.model.User;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 用户基本信息表读服务实现类
 * Date: 2017-06-23
 */
@Slf4j
@Service
public class UserReadService {

    private final UserDao userDao;

    @Autowired
    public UserReadService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Response<User> findById(Long Id) {
        try {
            return Response.ok(userDao.findById(Id));
        } catch (Exception e) {
            log.error("find user by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("user.find.fail");
        }
    }

    public Response<Optional<User>> findByOutId(Long outId){
        try {
            return Response.ok(Optional.fromNullable(userDao.findByOutId(outId)));
        } catch (Exception e) {
            log.error("find user by out id :{} failed,  cause:{}", outId, Throwables.getStackTraceAsString(e));
            return Response.fail("user.find.fail");
        }
    }
}