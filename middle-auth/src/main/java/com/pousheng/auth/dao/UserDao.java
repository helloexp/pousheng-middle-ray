package com.pousheng.auth.dao;

import com.pousheng.auth.model.User;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Author: songrenfei
 * Desc: 用户基本信息表Dao类
 * Date: 2017-06-23
 */
@Repository
public class UserDao extends MyBatisDao<User> {

    public User findByOutId(Long outId){
        return getSqlSession().selectOne(sqlId("findByOutId"),outId);
    }

}
