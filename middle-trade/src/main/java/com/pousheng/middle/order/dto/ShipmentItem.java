package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/22
 */
@Data
public class ShipmentItem extends BasicItemInfo implements Serializable{

    private static final long serialVersionUID = -5180878761122099289L;

    private Long skuOrderId;
    //已退货数量 (售后子单级别申请，数量一定大于等于quantity)
    //实际发货数量 -  已退货数量 = 剩余可退货数量
    private Integer refundQuantity;
    //数量
    private Integer quantity;
    //实际发货数量
    private Integer shipQuantity;
    //积分
    private Integer integral;
    //价格
    private Integer skuPrice;
    //商品优惠
    private Integer skuDiscount;
    //商品id
    private String itemId;
    //子单的外部订单id
    private String skuOutId;
    //是否是赠品
    private Boolean isGift;

    //分摊到发货单上面的平台优惠
    private Integer sharePlatformDiscount;

    //商品对应着哪个发货仓库
    private Long itemWarehouseId;
    private String itemWarehouseName;
}
