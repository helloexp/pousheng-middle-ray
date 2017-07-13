package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/4
 */
@Data
public class EditSubmitRefundInfo implements Serializable {


    private static final long serialVersionUID = -4455577798447116420L;


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
    //物流公司名称
    private String shipmentCorpName;
    //物流单号
    private String shipmentSerialNo;



    //操作类型 1：保存 2：提交
    private Integer operationType;


}
