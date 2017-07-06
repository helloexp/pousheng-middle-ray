package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
public class WarehouseShopGroupDaoTest extends BaseDaoTest{

    @Autowired
    private WarehouseShopGroupDao warehouseShopGroupDao;

    private WarehouseShopGroup warehouseShopGroup;

    @Before
    public void setUp() throws Exception {
        warehouseShopGroup = make();
        warehouseShopGroupDao.create(warehouseShopGroup);
        assertThat(warehouseShopGroup.getId(), notNullValue());
    }

    @Test
    public void deleteByGroupId() throws Exception {
        warehouseShopGroupDao.deleteByGroupId(warehouseShopGroup.getGroupId());
        List<WarehouseShopGroup> byRuleId = warehouseShopGroupDao.findByGroupId(warehouseShopGroup.getGroupId());
        assertThat(byRuleId.size(), is(0));
    }

    @Test
    public void findByGroupId() throws Exception {

        List<WarehouseShopGroup> byRuleId = warehouseShopGroupDao.findByGroupId(warehouseShopGroup.getGroupId());
        assertThat(byRuleId.size(), is(1));
    }

    @Test
    public void findByShopId() throws Exception {
        List<WarehouseShopGroup> byShopId = warehouseShopGroupDao.findByShopId(warehouseShopGroup.getShopId());
        assertThat(byShopId.size(), is(1));
    }

    private WarehouseShopGroup make() {
        WarehouseShopGroup warehouseShopGroup = new WarehouseShopGroup();


        warehouseShopGroup.setShopId(1L);
        warehouseShopGroup.setShopName("name");

        warehouseShopGroup.setGroupId(3L);

        warehouseShopGroup.setCreatedAt(new Date());

        warehouseShopGroup.setUpdatedAt(new Date());


        return warehouseShopGroup;
    }
}