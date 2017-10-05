package com.pousheng.auth.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.auth.dao.UserDao;
import com.pousheng.auth.model.MiddleUser;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: 用户基本信息表读服务实现类
 * Date: 2017-06-23
 */
@Slf4j
@Component
public class PsUserReadService {

    private final UserDao userDao;

    @Autowired
    public PsUserReadService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Response<MiddleUser> findById(Long Id) {
        try {
            return Response.ok(userDao.findById(Id));
        } catch (Exception e) {
            log.error("find user by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("user.find.fail");
        }
    }



    public Response<MiddleUser> findByName(String name) {
        try {
            return Response.ok(userDao.findByName(name));
        } catch (Exception e) {
            log.error("find user by name :{} failed,  cause:{}", name, Throwables.getStackTraceAsString(e));
            return Response.fail("user.find.fail");
        }
    }

    public Response<Optional<MiddleUser>> findByOutId(Long outId){
        try {
            log.info("start find middle user by outer id:{}",outId);
            return Response.ok(Optional.fromNullable(userDao.findByOutId(outId)));
        } catch (Exception e) {
            log.error("find user by out id :{} failed,  cause:{}", outId, Throwables.getStackTraceAsString(e));
            return Response.fail("user.find.fail");
        }
    }
}
