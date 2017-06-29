package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/26
 */
@Data
public class RefundItem implements Serializable{

    private static final long serialVersionUID = 4505554839511740470L;

    private Long skuOrderId;
    //条码：电商商品中外部id（也是中台货品条码）
    private String skuCode;
    //外部商品id：电商订单中商品id。
    private String outSkuCode;
    //商品名称
    private String skuName;
    //数量
    private Integer quantity;

    //价格
    private Integer skuPrice;

    //金额
    private Integer fee;

    //已发货数量（换货的发货）
    private Integer shippedQuantity;
}
