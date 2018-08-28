package com.pousheng.middle.order.impl.manager;

import com.pousheng.middle.order.impl.dao.ShipmentExtDao;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.parana.order.impl.dao.OrderShipmentDao;
import io.terminus.parana.order.impl.dao.ShipmentDao;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Created by songrenfei on 2017/7/6
 */
@Component
public class MiddleShipmentManager {

    private final ShipmentDao shipmentDao;

    private final OrderShipmentDao orderShipmentDao;

    @Autowired
    private ShipmentExtDao shipmentExtDao;


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

        Shipment newShipment = new Shipment();
        newShipment.setId(shipmentId);
        String shipmentCode = "SHP" + shipmentId;
        newShipment.setShipmentCode(shipmentCode);
        boolean updateSuccess = shipmentDao.update(newShipment);
        if (!updateSuccess) {
            throw new ServiceException("shipment.update.fail");
        } else {
            orderShipment.setShipmentId(shipmentId);
            orderShipment.setShipmentCode(shipmentCode);
            orderShipmentDao.create(orderShipment);
        }

        return shipmentId;
    }

    /**
     * 能指定排序的分页查询
     * @param offset
     * @param limit
     * @param sort
     * @param criteria
     * @return
     */
    public Paging<Shipment> paging(Integer offset, Integer limit,String sort,Map<String,Object> criteria){
        return shipmentExtDao.paging(offset,limit,sort,criteria);
    }


}
