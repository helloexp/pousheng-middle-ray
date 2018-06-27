package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2017/7/4
 */
@Data
public class EditSubmitRefundInfo implements Serializable {


    private static final long serialVersionUID = -4455577798447116420L;
    /**
     * 换货，退货退款，退款等提交的需要申请的售后sku以及数量的集合
     */
    private List<EditSubmitRefundItem> editSubmitRefundItems;

    /**
     * 换货商品集合
     */
    private List<EditSubmitChangeItem> editSubmitChangeItems;
    //退款金额
    private Long fee;
    //备注
    private String buyerNote;
    //发货仓ID
    private Long warehouseId;
    //物流公司代码
    private String shipmentCorpCode;
    //物流公司名称
    private String shipmentCorpName;
    //物流单号
    private String shipmentSerialNo;
    //发货单id
    private Long shipmentId;

    private String shipmentCode;


    //操作类型 1：保存 2：提交
    private Integer operationType;

    /**
     * 丢件补发类型的需要补发的商品条码
     */
    private List<ShipmentItem> lostItems;

    //退货仓id
    private String returnStockid;
    /**
     *关联订单类型
     */
    private Integer releOrderType;
    /**
     * 关联单号
     */
    private String  releOrderNo;
}
