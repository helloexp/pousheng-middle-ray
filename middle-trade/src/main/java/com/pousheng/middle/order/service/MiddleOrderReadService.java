package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.fsm.MiddleOrderCriteria;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;

import java.util.Map;

/**
 * 订单读服务
 * Created by songrenfei on 2017/6/16
 */
public interface MiddleOrderReadService {


    /**
     * 分页查询订单信息 支付返回订单基本信息
     * @param criteria 查询条件
     *                 pageNo 分页号, 从1开始
     *                 pageSize 分页大小
     *                 type {@link com.pousheng.middle.order.enums.OrderSource}
     *                 statusStr 状态,用,分割 {@link com.pousheng.middle.order.dto.fsm.MiddleOrderStatus}
     *                 outId 外部订单号
     *                 outCreateAtStartAt 下单开始时间
     *                 outCreateAtEndAt  下单结束时间
     *                 buyerName 买家名称
     *
     * @return 订单分页信息
     */
    Response<Paging<ShopOrder>> pagingShopOrder(MiddleOrderCriteria criteria);
}
