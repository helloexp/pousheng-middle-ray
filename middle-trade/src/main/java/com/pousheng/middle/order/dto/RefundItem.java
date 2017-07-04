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
    private Integer quantity;

    //价格
    private Integer skuPrice;

    //金额
    private Long fee;

    //已处理数量（换货的发货）
    private Integer alreadyHandleNumber;
}
