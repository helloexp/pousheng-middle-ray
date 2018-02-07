package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.ShipmentAmountDao;
import com.pousheng.middle.order.model.ShipmentAmount;
import com.pousheng.middle.order.service.ShipmentAmountWriteService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/2/7
 * pousheng-middle
 */
@Slf4j
@Service
public class ShipmentAmountWriteServiceImpl implements ShipmentAmountWriteService {
    @Autowired
    private ShipmentAmountDao shipmentAmountDao;
    @Override
    public Response<Long> create(ShipmentAmount shipmentAmount) {
        try{
            //更新订单状态逻辑,带事物
            shipmentAmountDao.create(shipmentAmount);
            return Response.ok(shipmentAmount.getId());
        }catch (ServiceException e1){
            log.error("failed to update order.cause:{}", Throwables.getStackTraceAsString(e1));
            return Response.fail(e1.getMessage());
        } catch (Exception e){
            log.error("failed to update order, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("order.update.fail");
        }
    }
}
