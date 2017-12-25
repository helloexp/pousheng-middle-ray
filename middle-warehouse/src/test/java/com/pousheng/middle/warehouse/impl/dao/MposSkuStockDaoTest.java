package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.MposSkuStock;
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
 * Desc: mpos下单sku锁定库存情况Dao 测试类
 * Date: 2017-12-23
 */
public class MposSkuStockDaoTest extends BaseDaoTest {



    @Autowired
    private MposSkuStockDao mposSkuStockDao;

    private MposSkuStock mposSkuStock;

    @Before
    public void init() {
        mposSkuStock = make();

        mposSkuStockDao.create(mposSkuStock);
        assertNotNull(mposSkuStock.getId());
    }

    @Test
    public void findById() {
        MposSkuStock mposSkuStockExist = mposSkuStockDao.findById(mposSkuStock.getId());

        assertNotNull(mposSkuStockExist);
    }

    @Test
    public void update() {
        // todo
        mposSkuStock.setShopId(2L);
        mposSkuStockDao.update(mposSkuStock);

        MposSkuStock  updated = mposSkuStockDao.findById(mposSkuStock.getId());
        assertEquals(updated.getShopId(), Long.valueOf(2));
    }

    @Test
    public void delete() {
        mposSkuStockDao.delete(mposSkuStock.getId());

        MposSkuStock deleted = mposSkuStockDao.findById(mposSkuStock.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        params.put("shopId", mposSkuStock.getShopId());
        Paging<MposSkuStock > mposSkuStockPaging = mposSkuStockDao.paging(0, 20, params);

        assertThat(mposSkuStockPaging.getTotal(), is(1L));
        assertEquals(mposSkuStockPaging.getData().get(0).getId(), mposSkuStock.getId());
    }

    private MposSkuStock make() {
        MposSkuStock mposSkuStock = new MposSkuStock();

        
        mposSkuStock.setWarehouseId(2L);
        
        mposSkuStock.setShopId(1l);
        
        mposSkuStock.setSkuCode("233");
        
        mposSkuStock.setLockedStock(3L);
        
        mposSkuStock.setCreatedAt(new Date());
        
        mposSkuStock.setUpdatedAt(new Date());
        

        return mposSkuStock;
    }

}