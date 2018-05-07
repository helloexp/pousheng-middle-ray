package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.ZoneContract;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.util.DateUtil.now;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * Author: songrenfei
 * Desc: 区部联系人表Dao 测试类
 * Date: 2018-04-04
 */
public class ZoneContractDaoTest extends BaseDaoTest {


    @Autowired
    private ZoneContractDao zoneContractDao;

    private ZoneContract zoneContract;

    @Before
    public void init() {
        zoneContract = make();

        zoneContractDao.create(zoneContract);
        assertNotNull(zoneContract.getId());
    }

    @Test
    public void findById() {
        ZoneContract zoneContractExist = zoneContractDao.findById(zoneContract.getId());

        assertNotNull(zoneContractExist);
    }

    @Test
    public void update() {
        // todo
        zoneContract.setUpdatedAt(now());
        zoneContractDao.update(zoneContract);

        ZoneContract updated = zoneContractDao.findById(zoneContract.getId());
        // todo
        //assertEquals(updated.getHasDisplay(), Boolean.TRUE);
    }

    @Test
    public void delete() {
        zoneContractDao.delete(zoneContract.getId());

        ZoneContract deleted = zoneContractDao.findById(zoneContract.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        //todo
        //params.put("userId", zoneContract.getUserId());
        Paging<ZoneContract> zoneContractPaging = zoneContractDao.paging(0, 20, params);

        assertThat(zoneContractPaging.getTotal(), is(1L));
        assertEquals(zoneContractPaging.getData().get(0).getId(), zoneContract.getId());
    }

    private ZoneContract make() {
        ZoneContract zoneContract = new ZoneContract();


        zoneContract.setZoneId("zone001");

        zoneContract.setZoneName("杭州区部");

        zoneContract.setName("茶派");

        zoneContract.setEmail("fewf23@163.com");

        zoneContract.setPhone("1777222231");

        zoneContract.setGroup(1);

        zoneContract.setStatus(1);

        zoneContract.setCreatedAt(new Date());

        zoneContract.setUpdatedAt(new Date());


        return zoneContract;
    }

}