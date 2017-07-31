package com.pousheng.middle.order.service;

import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;

/**
 * Created by songrenfei on 2017/7/6
 */
public interface MiddleShipmentWriteService {

    /**
     * @param shipment  发货单
     * @param orderId  订单id
     * @param afterSaleOrderId 订单对应的级别
     * @return  新创建发货单的id
     */
    Response<Long> createForAfterSale(Shipment shipment, Long orderId,Long afterSaleOrderId);

}
