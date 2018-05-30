package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.MiddleChangeReceiveInfo;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Refund;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/26
 */
public interface MiddleRefundWriteService {


    /**
     * 创建退款申请单,同时也会创建退款单(处于待退款状态), 同时也会创建相应的订单和退款申请单的关联关系
     *
     * @param refund 退款申请单信息
     * @param  orderIds 关联的(子)订单id列表
     * @param  orderLevel 订单对应的级别
     * @return  退款申请单id
     */
    Response<Long> create(Refund refund, List<Long> orderIds, OrderLevel orderLevel);


    /**
     * 更新订单的收货信息
     * @param refundId 店铺订单主键
     * @param middleChangeReceiveInfo 编辑的收货信息
     * @return
     */
    public Response<Boolean> updateReceiveInfos(long refundId, MiddleChangeReceiveInfo middleChangeReceiveInfo);

}
