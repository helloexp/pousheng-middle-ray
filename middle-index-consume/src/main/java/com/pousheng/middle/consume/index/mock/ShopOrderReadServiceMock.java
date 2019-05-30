package com.pousheng.middle.consume.index.mock;

import com.google.common.base.Optional;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;

import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-17 17:46<br/>
 */
public class ShopOrderReadServiceMock implements ShopOrderReadService {
    @Override
    public Response<ShopOrder> findById(Long aLong) {
        return null;
    }

    @Override
    public Response<List<ShopOrder>> findByIds(List<Long> list) {
        return null;
    }

    @Override
    public Response<Paging<ShopOrder>> findBy(Integer integer, Integer integer1, OrderCriteria orderCriteria) {
        return null;
    }

    @Override
    public Response<Optional<ShopOrder>> findByOutIdAndOutFrom(String s, String s1) {
        return null;
    }

    @Override
    public Response<List<ShopOrder>> findByOutId(String s) {
        return null;
    }

    @Override
    public Response<ShopOrder> findByOrderCode(String s) {
        return null;
    }
}
