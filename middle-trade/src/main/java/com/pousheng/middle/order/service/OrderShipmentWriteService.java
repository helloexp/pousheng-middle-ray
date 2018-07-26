package com.pousheng.middle.order.service;

import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;

/**
 * Created by penghui on 2018/3/20
 */
public interface OrderShipmentWriteService {

    /**
     * 更新订单发货单关联
     * @param orderShipment
     * @return
     */
    Response<Boolean> update(OrderShipment orderShipment);

    /**
     * 更新部分发货
     * @param shipmentId
     * @param partShip
     * @return
     */
    Response<Boolean> updatePartShip(Long shipmentId, Boolean partShip);




}
