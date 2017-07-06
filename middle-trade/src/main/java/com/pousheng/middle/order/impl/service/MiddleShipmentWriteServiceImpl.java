package com.pousheng.middle.order.impl.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.manager.MiddleShipmentManager;
import com.pousheng.middle.order.service.MiddleShipmentWriteService;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by songrenfei on 2017/7/6
 */
@Slf4j
@Service
public class MiddleShipmentWriteServiceImpl implements MiddleShipmentWriteService{

    @Autowired
    private MiddleShipmentManager middleShipmentManager;

    @Override
    public Response<Long> createForAfterSale(Shipment shipment, Long orderId, Long afterSaleOrderId) {
        try {
            shipment.setStatus(MoreObjects.firstNonNull(shipment.getStatus(), 1));
            OrderShipment orderShipment = new OrderShipment();
            orderShipment.setOrderId(orderId);
            orderShipment.setAfterSaleOrderId(afterSaleOrderId);
            orderShipment.setOrderLevel(OrderLevel.SHOP);
            orderShipment.setStatus(shipment.getStatus());
            orderShipment.setType(shipment.getType());
            Long shipmentId = middleShipmentManager.create(shipment, orderShipment);
            return Response.ok(shipmentId);
        } catch (Exception e) {
            log.error("failed to create {}, cause:{}", shipment, Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.create.fail");
        }

    }
}
