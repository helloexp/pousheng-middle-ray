package com.pousheng.middle.order.dto;

import io.terminus.parana.order.dto.RefundCriteria;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by songrenfei on 2017/8/10
 */
@Data
public class MiddleRefundCriteria extends RefundCriteria implements Serializable{

    private static final long serialVersionUID = -848661870245726484L;

    private Long id;

    //售后单类型
    private Integer refundType;


    //排除的售后单类型
    private Integer excludeRefundType;


    /**
     * 关联单号
     */
    private String releOrderCode;

    /**
     * 售后单号
     */
    private String refundCode;


    private String orderCode;

    private Integer refundFlag;

    /**
     * 退回快递单号
     */
    private String shipmentSerialNo;

    /**
     * 是否完善退货物流
     */
    private Integer completeReturn;

    /**
     * 退货入库时间
     */
    private Date hkReturnDoneAtStart;
    private Date hkReturnDoneAtEnd;


    private Integer pageSize;
}
