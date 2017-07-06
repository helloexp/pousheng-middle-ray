package com.pousheng.middle.order.impl.manager;

import io.terminus.common.exception.ServiceException;
import io.terminus.parana.order.impl.dao.OrderShipmentDao;
import io.terminus.parana.order.impl.dao.ShipmentDao;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by songrenfei on 2017/7/6
 */
@Component
public class MiddleShipmentManager {

    private final ShipmentDao shipmentDao;

    private final OrderShipmentDao orderShipmentDao;

    @Autowired
    public MiddleShipmentManager(ShipmentDao shipmentDao, OrderShipmentDao orderShipmentDao) {
        this.shipmentDao = shipmentDao;
        this.orderShipmentDao = orderShipmentDao;
    }

    @Transactional
    public Long create(Shipment shipment, OrderShipment  orderShipment) {
        boolean success = shipmentDao.create(shipment);
        if (!success) {
            throw new ServiceException("shipment.create.fail");
        }
        Long shipmentId = shipment.getId();
        orderShipment.setShipmentId(shipmentId);
        orderShipmentDao.create(orderShipment);
        return shipmentId;
    }

}
