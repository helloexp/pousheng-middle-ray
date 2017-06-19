package com.pousheng.middle.web.order;

import com.google.common.base.Strings;
import com.pousheng.middle.order.dto.fsm.MiddleOrderCriteria;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Mail: F@terminus.io
 * Data: 16/6/28
 * Author: yangzefeng
 */
@Controller
@Slf4j
public class AdminOrderReader {

    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;

    @RequestMapping(value = "/api/order/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<Paging<ShopOrder>> findBy(MiddleOrderCriteria criteria) {

        if (criteria != null && !Strings.isNullOrEmpty(criteria.getShopName())) {
            Response<Shop> shopR = shopReadService.findByName(criteria.getShopName());
            if (!shopR.isSuccess()) {
                log.error("fail to find shop by name {}, error code:{}",
                        criteria.getShopName(), shopR.getError());
                return Response.ok(Paging.empty(ShopOrder.class));
            } else {
                Shop shop = shopR.getResult();
                criteria.setShopId(shop.getId());
            }
        }
        return middleOrderReadService.pagingShopOrder(criteria);
    }


    @RequestMapping(value = "/api/order/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<OrderDetail> detail(@PathVariable("id") Long id) {
        return orderReadLogic.orderDetail(id);
    }
}