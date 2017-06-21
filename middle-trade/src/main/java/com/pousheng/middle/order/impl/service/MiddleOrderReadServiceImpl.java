package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 订单读服务
 * Created by songrenfei on 2017/6/16
 */
@Slf4j
@Service
@RpcProvider
public class MiddleOrderReadServiceImpl implements MiddleOrderReadService {

    @Autowired
    private ShopOrderDao shopOrderDao;


    @Override
    public Response<Paging<ShopOrder>> pagingShopOrder(MiddleOrderCriteria criteria) {
        try {
            Paging<ShopOrder> paging = shopOrderDao.paging(criteria.getOffset(),criteria.getLimit(),criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging shop order, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.find.fail");
        }
    }
}
