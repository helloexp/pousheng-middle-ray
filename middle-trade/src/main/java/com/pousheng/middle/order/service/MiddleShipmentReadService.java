package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.MiddleShipmentCriteria;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;

/**
 * 发货单读服务
 * @author tanlongjun
 */
public interface MiddleShipmentReadService {

    /**
     * 分页查询发货单
     * @param criteria  条件
     * @return
     */
    Response<Paging<Shipment>> pagingShipment(MiddleShipmentCriteria criteria);

}
