package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.service.OrderShipmentWriteService;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.OrderShipmentDao;
import io.terminus.parana.order.model.OrderShipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by penghui on 2018/3/20
 */
@Service
@Slf4j
public class OrderShipmentWriteServiceImpl implements OrderShipmentWriteService {

    @Autowired
    private OrderShipmentDao orderShipmentDao;

    @Override
    public Response<Boolean> update(OrderShipment orderShipment) {
        try{
            return Response.ok(orderShipmentDao.update(orderShipment));
        }catch (Exception e){
            log.error("update ordershipment:{} failed,cause:{}",orderShipment, Throwables.getStackTraceAsString(e));
            return Response.fail("update.order.shipment.fail");
        }
    }
}
