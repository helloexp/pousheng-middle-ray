package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * 创建收货单提交信息
 * Created by songrenfei on 2017/6/28
 */
@Data
public class SubmitRefundInfo extends EditSubmitRefundInfo {

    /**
     *  售后类型
     *  @see com.pousheng.middle.order.enums.MiddleRefundType
     */
    private Integer refundType;

    //交易单id
    private Long orderId;
    //发货单id
    private Long shipmentId;

    //操作类型 1：保存 2：提交
    private Integer operationType;




}
