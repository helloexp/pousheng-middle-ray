package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.OrderShipmentDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/20
 */
@Slf4j
@Service
@RpcProvider
public class OrderShipmentReadServiceImpl implements OrderShipmentReadService{

    @Autowired
    private OrderShipmentDao orderShipmentDao;


    @Override
    public Response<List<OrderShipment>> findByOrderIdAndOrderLevel(Long orderId, OrderLevel orderLevel) {
        try {
            List<OrderShipment> orderShipments = orderShipmentDao.findByOrderIdAndOrderType(orderId, orderLevel.getValue());
            if (CollectionUtils.isEmpty(orderShipments)) {
                return Response.ok(Collections.<OrderShipment>emptyList());
            }
            return Response.ok(orderShipments);
        }catch (Exception e) {
            log.error("failed to find order shipment(orderId={}, orderLevel={}), cause:{}",
                    orderId, orderLevel, Throwables.getStackTraceAsString(e));
            return Response.fail("order.shipment.find.fail");
        }
    }
}
