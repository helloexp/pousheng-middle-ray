package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * 创建收货单提交信息
 * Created by songrenfei on 017/6/28
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

    //交易单号
    private String orderCode;
    //发货单id
    private Long shipmentId;
    //发货单号
    private String shipmentCode;
    //换货收货人信息
    private MiddleChangeReceiveInfo middleChangeReceiveInfo;


    //售后单id
    private String outAfterSaleOrderId;


}
