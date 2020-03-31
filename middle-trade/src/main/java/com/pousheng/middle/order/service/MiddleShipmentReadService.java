package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.MiddleShipmentCriteria;
import com.pousheng.middle.order.dto.ShipmentRejectReason;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;

import java.util.Date;
import java.util.Map;

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
    /**
     * 分页查询发货单
     * @param sort 排序规则  如 id asc
     * @param criteria 查询条件
     * @return
     */
    Response<Paging<Shipment>> paging(Integer offset, Integer limit,String sort, Map<String,Object> criteria);

    /**
     * 分页查询拒单历史原因
     * @param pageNo
     * @param pageSize
     * @param criteria
     * @return
     */
    Response<Paging<ShipmentRejectReason>> pagingRejectReason(Integer pageNo, Integer pageSize, Map<String, Object> criteria);

    Response<Paging<Shipment>> findShipmentCustom(Integer status, Date updatedAtStart, Date updatedAtEnd, Long limit, Integer offset);
}
