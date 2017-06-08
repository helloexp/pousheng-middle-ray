/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseAddress;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * 菜鸟地址测试
 *
 * Author  : panxin
 * Date    : 10:09 AM 3/7/16
 * Mail    : panxin@terminus.io
 */
public class WarehouseAddressDaoTest extends BaseDaoTest{

    @Autowired
    private WarehouseAddressDao warehouseAddressDao;

    private WarehouseAddress u;

    @Before
    public void setUp() throws Exception {
        u = new WarehouseAddress();
        u.setEnglishName("China");
        u.setPid(2L);
        u.setId(1L);
       assertThat(warehouseAddressDao.createWithId(u), is(true));
    }

    /**
     * 参照 schema.sql 的测试数据
     */
    @Test
    public void testFind(){
        WarehouseAddress top = warehouseAddressDao.findById(u.getId());
        assertThat(top.getEnglishName(), is(u.getEnglishName()));

        List<WarehouseAddress> lv1 = warehouseAddressDao.findByPid(u.getPid());
        assertThat(lv1.size(), is(1));
    }

}
