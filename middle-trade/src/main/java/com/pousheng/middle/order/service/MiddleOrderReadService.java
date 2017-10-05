package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.model.MiddleShopOrder;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;

import java.util.List;

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
     *                 shopName 订单来源
     *                 statusStr 状态,用,分割 {@link com.pousheng.middle.order.dto.fsm.MiddleOrderStatus}
     *                 outId 外部订单号
     *                 outCreatedStartAt 下单开始时间
     *                 outCreatedEndAt  下单结束时间
     *                 buyerName 买家名称
     *
     * @return 订单分页信息
     */
    Response<Paging<ShopOrder>> pagingShopOrder(MiddleOrderCriteria criteria);

    /**
     * 分页查询订单信息 支付返回订单基本信息
     * @param criteria 查询条件
     *                 pageNo 分页号, 从1开始
     *                 pageSize 分页大小
     *                 shopName 订单来源
     *                 statusStr 状态,用,分割 {@link com.pousheng.middle.order.dto.fsm.MiddleOrderStatus}
     *                 outId 外部订单号
     *                 outCreatedStartAt 下单开始时间
     *                 outCreatedEndAt  下单结束时间
     *                 buyerName 买家名称
     *                 mobile  手机号
     * @return 订单分页信息
     */
    Response<Paging<MiddleShopOrder>> pagingMiddleShopOrder(MiddleOrderCriteria criteria);

    /**
     * 查询订单相关发票信息
     * @param orderId 订单（子单）id
     * @param orderLevel {@link OrderLevel}
     * @return 发票信息
     */
    Response<List<Invoice>> findInvoiceInfo(Long orderId, OrderLevel orderLevel);

    /**
     * 查询订单相关收货地址信息
     * @param orderId 订单（子单）id
     * @param orderLevel {@link OrderLevel}
     * @return 发票信息
     */
    Response<List<OrderReceiverInfo>> findOrderReceiverInfo(Long orderId, OrderLevel orderLevel);



}
