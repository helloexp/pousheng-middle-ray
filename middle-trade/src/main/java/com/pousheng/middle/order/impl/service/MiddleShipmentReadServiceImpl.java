package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.MiddleShipmentCriteria;
import com.pousheng.middle.order.impl.dao.ShipmentExtDao;
import com.pousheng.middle.order.impl.manager.MiddleShipmentManager;
import com.pousheng.middle.order.service.MiddleShipmentReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 发货单读服务
 * @author tanlongjun
 */
@Slf4j
@Service
public class MiddleShipmentReadServiceImpl implements MiddleShipmentReadService {

    @Autowired
    private ShipmentExtDao shipmentExtDao;

    @Autowired
    private MiddleShipmentManager middleShipmentManager;

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



    @Override
    public Response<Paging<Shipment>> paging(Integer offset, Integer limit,String sort, Map<String, Object> criteria) {
        try{
            return Response.ok(middleShipmentManager.paging(offset,limit,sort, criteria));
        }catch (Exception e){
            log.error("failed to paging shipment.sort:{},criteria:{}",sort,criteria,e);
        }
        return Response.fail("failed to paging shipment");
    }
}
