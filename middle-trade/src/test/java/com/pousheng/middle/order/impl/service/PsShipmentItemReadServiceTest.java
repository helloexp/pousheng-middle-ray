package com.pousheng.middle.order.impl.service;

import com.pousheng.middle.order.dto.ShipmentItemCriteria;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.ShipmentItemDao;
import io.terminus.parana.order.model.ShipmentItem;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/9/12下午7:43
 */
public class PsShipmentItemReadServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {

        @MockBean
        private ShipmentItemDao shipmentItemDao;

        @SpyBean
        private PsShipmentItemReadServiceImpl psShipmentItemReadService;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return PsShipmentItemReadServiceTest.MockitoBeans.class;
    }

    ShipmentItemDao shipmentItemDao;
    PsShipmentItemReadServiceImpl psShipmentItemReadService;


    @Override
    protected void init() {
        shipmentItemDao = get(ShipmentItemDao.class);
        psShipmentItemReadService = get(PsShipmentItemReadServiceImpl.class);
    }

    @Test
    public void findShipmentItems() {
        ShipmentItemCriteria criteria = new ShipmentItemCriteria();

        Response<List<ShipmentItem>> r = psShipmentItemReadService.findShipmentItems(criteria);

        assertThat(r.isSuccess(),is(true));
    }

}
