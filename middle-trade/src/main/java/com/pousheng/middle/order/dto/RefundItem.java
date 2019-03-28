package com.pousheng.middle.order.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/26
 */
@Data
public class RefundItem extends BasicItemInfo implements Serializable {

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

    //换货商品所选的仓库id
    private Long exchangeWarehouseId;
    //换货商品所选择的仓库名称
    private String exchangeWarehouseName;
    //商品id
    private String itemId;
    //换货时存放的需要申请售后的skuCode
    private String refundSkuCode;
    //购买数量
    private Integer quantity;

    //子售后单id
    private String skuAfterSaleId;

    //发货单上对应的平台分摊优惠
    private Integer sharePlatformDiscount;

    /**
     * 最终退货数量
     */
    private Integer finalRefundQuantity;
}
