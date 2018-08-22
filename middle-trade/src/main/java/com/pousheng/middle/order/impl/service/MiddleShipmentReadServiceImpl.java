package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.MiddleShipmentCriteria;
import com.pousheng.middle.order.impl.dao.ShipmentExtDao;
import com.pousheng.middle.order.service.MiddleShipmentReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author tanlongjun
 */
@Slf4j
@Service
public class MiddleShipmentReadServiceImpl implements MiddleShipmentReadService {

    @Autowired
    private ShipmentExtDao shipmentExtDao;

    @Override
    public Response<Paging<Shipment>> pagingShipment(MiddleShipmentCriteria criteria){
        try {

            Paging<Shipment> paging= shipmentExtDao.pagingExt(criteria.getOffset(),criteria.getLimit(),criteria.toMap());;
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging shipment, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.find.fail");
        }

    }

}
