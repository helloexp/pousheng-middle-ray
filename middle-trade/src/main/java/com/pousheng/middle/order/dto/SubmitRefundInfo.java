package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * 创建收货单提交信息
 * Created by songrenfei on 2017/6/28
 */
@Data
public class SubmitRefundInfo {

    /**
     *  售后类型
     *  @see com.pousheng.middle.order.enums.MiddleRefundType
     */
    private Integer refundType;

    //交易单id
    private Long orderId;
    //发货单id
    private Long shipmentId;
    //商品编码和数量 (退货)
    private String refundSkuCode;
    //数量 (退货)
    private Integer refundQuantity;
    //商品编码和数量 (换货)
    private String changeSkuCode;
    //数量 (换货)
    private Integer changeQuantity;
    //退款金额
    private Long fee;
    //备注
    private String buyerNote;
    //发货仓ID
    private Long warehouseId;
    //物流公司代码
    private String shipmentCorpCode;
    //物流单号
    private String shipmentSerialNo;




}
