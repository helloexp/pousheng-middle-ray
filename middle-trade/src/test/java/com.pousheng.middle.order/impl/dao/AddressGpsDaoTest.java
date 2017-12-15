package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.AddressGps;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * Author: songrenfei
 * Desc: 地址定位信息表Dao 测试类
 * Date: 2017-12-15
 */
public class AddressGpsDaoTest extends BaseDaoTest {



    @Autowired
    private AddressGpsDao addressGpsDao;

    private AddressGps addressGps;

    @Before
    public void init() {
        addressGps = make();

        addressGpsDao.create(addressGps);
        assertNotNull(addressGps.getId());
    }

    @Test
    public void findById() {
        AddressGps addressGpsExist = addressGpsDao.findById(addressGps.getId());

        assertNotNull(addressGpsExist);
    }

    @Test
    public void update() {
        addressGps.setBusinessId(2l);
        addressGpsDao.update(addressGps);

        AddressGps  updated = addressGpsDao.findById(addressGps.getId());
        assertEquals(updated.getBusinessId(), Long.valueOf(2























        ));
    }

    @Test
    public void delete() {
        addressGpsDao.delete(addressGps.getId());

        AddressGps deleted = addressGpsDao.findById(addressGps.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("businessId", addressGps.getBusinessId());
        Paging<AddressGps > addressGpsPaging = addressGpsDao.paging(0, 20, params);

        assertThat(addressGpsPaging.getTotal(), is(1L));
        assertEquals(addressGpsPaging.getData().get(0).getId(), addressGps.getId());
    }

    private AddressGps make() {
        AddressGps addressGps = new AddressGps();

        
        addressGps.setBusinessId(4L);
        
        addressGps.setBusinessType(1);
        
        addressGps.setLongitude("223.23");
        
        addressGps.setLatitude("2323.23");
        
        addressGps.setProvince("浙江省");
        
        addressGps.setProvinceId(23l);
        
        addressGps.setCity("杭州市");
        
        addressGps.setCityId(3L);
        
        addressGps.setRegion("滨江区");
        
        addressGps.setRegionId(2l);
        
        addressGps.setStreet("浦沿街道");
        
        addressGps.setStreetId(32l);
        
        addressGps.setDetail("明细");
        
        addressGps.setCreatedAt(new Date());
        
        addressGps.setUpdatedAt(new Date());
        

        return addressGps;
    }

}