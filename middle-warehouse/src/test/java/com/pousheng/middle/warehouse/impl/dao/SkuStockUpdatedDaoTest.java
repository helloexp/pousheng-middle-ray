package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.SkuStockUpdated;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Date;
import java.util.List;
import static org.junit.Assert.*;

public class SkuStockUpdatedDaoTest extends BaseDaoTest {

    @SpyBean
    private SkuStockUpdatedDao skuStockUpdatedDao;

    @Test
    public void findWaiteHandle() {
        SkuStockUpdated skuStockUpdated = new SkuStockUpdated();
        skuStockUpdated.setSkuCode("080874698961");
        skuStockUpdated.setCreatedAt(new Date());
        skuStockUpdated.setUpdatedAt(new Date());

        skuStockUpdatedDao.create(skuStockUpdated);
        List list = skuStockUpdatedDao.findWaiteHandle();
        //System.out.println(list.toString());
        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void deleteAll() {
        skuStockUpdatedDao.deleteAll();
        Assert.assertTrue(skuStockUpdatedDao.deleteAll());
    }
}