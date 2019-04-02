package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

import com.pousheng.middle.order.model.ExpressCode;

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
    //丢件补发信息
    private List<RefundItem> lostRefundItems;
	// 2019.04.02 所有的快遞公司訊息
	private List<ExpressCode> expressItems;

    /**
     * 是否为新建售后单
     */
    private Boolean isToCreate;
}
