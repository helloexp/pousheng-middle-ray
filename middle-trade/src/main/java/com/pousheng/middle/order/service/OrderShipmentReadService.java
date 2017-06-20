package com.pousheng.middle.order.service;

import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/20
 */
public interface OrderShipmentReadService {


    /**
     * 根据(子)订单id和级别查找对应的发货单列表
     *
     * @param orderId    (子)订单id
     * @param orderLevel 级别
     * @return 对应的发货单列表
     */
    Response<List<OrderShipment>> findByOrderIdAndOrderLevel(Long orderId, OrderLevel orderLevel);
}
