package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/26
 */
@Data
public class EditRefundItem extends RefundItem implements Serializable{


    private static final long serialVersionUID = 4434229644398150221L;
    //已退货数量 (售后子单级别申请，数量一定大于等于quantity)
    //发货数量 -  已退货数量 = 剩余可退货数量
    private Integer refundQuantity;
    //数量
    private Integer quantity;
}
