package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/26
 */
@Data
public class RefundItem extends BasicItemInfo implements Serializable{

    private static final long serialVersionUID = 4505554839511740470L;

    private Long skuOrderId;
    //数量
    private Integer applyQuantity;

    //价格
    private Integer skuPrice;

    //金额
    private Long fee;

    //折扣
    private Integer skuDiscount;

    //已处理数量（换货的发货）
    private Integer alreadyHandleNumber;
    //商品id
    private String itemId;
    //换货时存放的需要申请售后的skuCode
    private String refundSkuCode;
    //购买数量
    private Integer quantity;
}
