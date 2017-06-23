package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/22
 */
@Data
public class ShipmentItem implements Serializable{

    private static final long serialVersionUID = -5180878761122099289L;

    private Long skuOrderId;
    //条码：电商商品中外部id（也是中台货品条码）
    private String skuCode;
    //外部商品id：电商订单中商品id。
    private String outSkuCode;
    //商品名称
    private String skuName;
    //数量
    private Integer quantity;
    //积分
    private Integer integral;
    //价格
    private Integer skuPrice;
    //商品优惠
    private Integer skuDiscount;
    //分摊优惠
    private Integer apportionDiscount;
    //商品净价
    private Integer cleanPrice;
    //商品总净价
    private Integer cleanFee;



}
