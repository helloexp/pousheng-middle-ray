package com.pousheng.middle.order.impl.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.manager.MiddleShipmentManager;
import com.pousheng.middle.order.service.MiddleShipmentWriteService;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.model.*;
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
    private ShopOrderDao shopOrderDao;

    @Autowired
    private MiddleShipmentManager middleShipmentManager;

    @Override
    public Response<Long> createForAfterSale(Shipment shipment, OrderRefund orderRefund, Long afterSaleOrderId) {
        try {
            shipment.setStatus(MoreObjects.firstNonNull(shipment.getStatus(), 1));
            ShopOrder order = shopOrderDao.findById(orderRefund.getOrderId());
            OrderShipment orderShipment = new OrderShipment();
            orderShipment.setOrderId(orderRefund.getOrderId());
            orderShipment.setOrderCode(orderRefund.getOrderCode());
            orderShipment.setAfterSaleOrderId(afterSaleOrderId);
            orderShipment.setAfterSaleOrderCode(orderRefund.getRefundCode());
            orderShipment.setOrderLevel(OrderLevel.SHOP);
            orderShipment.setStatus(shipment.getStatus());
            orderShipment.setType(shipment.getType());
            orderShipment.setShopId(shipment.getShopId());
            orderShipment.setShopName(shipment.getShopName());
            orderShipment.setShipWay(shipment.getShipWay());
            orderShipment.setShipId(shipment.getShipId());
            orderShipment.setOrderTime(order.getOutCreatedAt());
            Long shipmentId = middleShipmentManager.create(shipment, orderShipment);
            return Response.ok(shipmentId);
        } catch (Exception e) {
            log.error("failed to create {}, cause:{}", shipment, Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.create.fail");
        }

    }

}
