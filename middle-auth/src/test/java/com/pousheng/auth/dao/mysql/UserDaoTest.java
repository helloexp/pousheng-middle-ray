package com.pousheng.auth.dao.mysql;

import com.google.common.collect.Maps;
import com.pousheng.auth.dao.BaseDaoTest;
import com.pousheng.auth.dao.UserDao;
import com.pousheng.auth.model.User;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


/**
 * Author: songrenfei
 * Desc: 用户基本信息表Dao 测试类
 * Date: 2017-06-23
 */
public class UserDaoTest extends BaseDaoTest {



    @Autowired
    private UserDao userDao;

    private User user;

    @Before
    public void init() {
        user = make();

        userDao.create(user);
        assertNotNull(user.getId());
    }

    @Test
    public void findById() {
        User userExist = userDao.findById(user.getId());

        assertNotNull(userExist);
    }

    @Test
    public void update() {
        user.setOutId(1L);
        userDao.update(user);

        User  updated = userDao.findById(user.getId());
        assertEquals(updated.getOutId(), Long.valueOf(1));
    }

    @Test
    public void delete() {
        userDao.delete(user.getId());

        User deleted = userDao.findById(user.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("outId", user.getOutId());
        Paging<User > userPaging = userDao.paging(0, 20, params);

        assertThat(userPaging.getTotal(), is(1L));
        assertEquals(userPaging.getData().get(0).getId(), user.getId());
    }

    private User make() {
        User user = new User();


        Map<String, String> extra = Maps.newHashMap();
        extra.put("213","23");

        user.setOutId(43L);
        
        user.setName("SONGRENFEI");
        
        user.setExtra(extra);
        
        user.setCreatedAt(new Date());
        
        user.setUpdatedAt(new Date());
        

        return user;
    }

}