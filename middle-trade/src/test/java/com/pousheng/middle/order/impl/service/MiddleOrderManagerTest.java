package com.pousheng.middle.order.impl.service;

import com.pousheng.middle.order.impl.dao.ShopOrderExtDao;
import com.pousheng.middle.order.impl.manager.MiddleOrderManager;
import io.terminus.parana.order.impl.dao.OrderReceiverInfoDao;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.impl.dao.SkuOrderDao;
import io.terminus.parana.order.model.OrderReceiverInfo;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class MiddleOrderManagerTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {

        @MockBean
        private OrderReceiverInfoDao orderReceiverInfoDao;

        @MockBean
        private ShopOrderExtDao shopOrderExtDao;

        @MockBean
        private SkuOrderDao skuOrderDao;
        @MockBean
        private ShopOrderDao shopOrderDao;

        @SpyBean
        private MiddleOrderManager middleOrderManager;
    }


    @Override
    protected Class<?> mockitoBeans() {
        return MiddleOrderManagerTest.MockitoBeans.class;
    }

    MiddleOrderManager middleOrderManager;
    ShopOrderExtDao shopOrderExtDao;
    OrderReceiverInfoDao orderReceiverInfoDao;

    @Override
    protected void init() {
        middleOrderManager = get(MiddleOrderManager.class);
        shopOrderExtDao = get(ShopOrderExtDao.class);
        orderReceiverInfoDao = get(OrderReceiverInfoDao.class);
    }


    @Test
    public void updateReceiverInfoAndBuyerNote() {
        when(orderReceiverInfoDao.update(any())).thenReturn(true);
        when(shopOrderExtDao.update(any())).thenReturn(true);
        middleOrderManager.updateReceiverInfoAndBuyerNote(123L,new OrderReceiverInfo(),"323");
    }

}