package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.ShipmentItemCriteria;
import com.pousheng.middle.order.service.PsShipmentItemReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.ShipmentItemDao;
import io.terminus.parana.order.model.ShipmentItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.Collections;
import java.util.List;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/9/12下午6:58
 */
@Slf4j
@Service
@RpcProvider
public class PsShipmentItemReadServiceImpl implements PsShipmentItemReadService {

    @Autowired
    private ShipmentItemDao shipmentItemDao;

    @Override
    public Response<List<ShipmentItem>> findShipmentItems(ShipmentItemCriteria criteria) {
       try {
           List<ShipmentItem> list = shipmentItemDao.findShipmentItems(criteria.toMap());
           if (CollectionUtils.isEmpty(list)) {
               return Response.ok(Collections.<ShipmentItem>emptyList());
           }
           return Response.ok(list);
       } catch (Exception e) {
           log.error("failed to find shipment item by {}, cause:{}", criteria.toString(), Throwables.getStackTraceAsString(e));
           return Response.fail("shipment.item.find.fail");
       }
    }
}
