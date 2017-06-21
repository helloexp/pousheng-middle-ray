package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.ShipmentDto;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.ShopOrder;

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

    /**
     * 发货单分页列表, 供开放平台使用
     *
     * @param criteria 发货查询条件
     * @return 分页发货单封装信息
     */
    Response<Paging<ShipmentDto>> findBy(OrderShipmentCriteria criteria);
}
