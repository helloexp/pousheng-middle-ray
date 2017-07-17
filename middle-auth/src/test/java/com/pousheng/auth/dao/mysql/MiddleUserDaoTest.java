package com.pousheng.auth.dao.mysql;

import com.google.common.collect.Maps;
import com.pousheng.auth.dao.BaseDaoTest;
import com.pousheng.auth.dao.UserDao;
import com.pousheng.auth.model.MiddleUser;
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
public class MiddleUserDaoTest extends BaseDaoTest {



    @Autowired
    private UserDao userDao;

    private MiddleUser middleUser;

    @Before
    public void init() {
        middleUser = make();

        userDao.create(middleUser);
        assertNotNull(middleUser.getId());
    }

    @Test
    public void findById() {
        MiddleUser middleUserExist = userDao.findById(middleUser.getId());

        assertNotNull(middleUserExist);
    }

    @Test
    public void update() {
        middleUser.setOutId(1L);
        userDao.update(middleUser);

        MiddleUser updated = userDao.findById(middleUser.getId());
        assertEquals(updated.getOutId(), Long.valueOf(1));
    }

    @Test
    public void delete() {
        userDao.delete(middleUser.getId());

        MiddleUser deleted = userDao.findById(middleUser.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("outId", middleUser.getOutId());
        Paging<MiddleUser> userPaging = userDao.paging(0, 20, params);

        assertThat(userPaging.getTotal(), is(1L));
        assertEquals(userPaging.getData().get(0).getId(), middleUser.getId());
    }

    private MiddleUser make() {
        MiddleUser middleUser = new MiddleUser();


        Map<String, String> extra = Maps.newHashMap();
        extra.put("213","23");

        middleUser.setOutId(43L);
        
        middleUser.setName("SONGRENFEI");
        
        middleUser.setExtra(extra);
        
        middleUser.setCreatedAt(new Date());
        
        middleUser.setUpdatedAt(new Date());
        

        return middleUser;
    }

}