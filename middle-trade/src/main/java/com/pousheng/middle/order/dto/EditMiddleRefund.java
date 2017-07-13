package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/26
 */
@Data
public class EditMiddleRefund extends RichMiddleRefund implements Serializable {


    private static final long serialVersionUID = 890811632360236644L;
    //退货信息
    private List<EditRefundItem> refundItems;
    //换货信息
    private List<RefundItem> shipmentItems;

    /**
     * 是否为新建售后单
     */
    private Boolean isToCreate;
}
