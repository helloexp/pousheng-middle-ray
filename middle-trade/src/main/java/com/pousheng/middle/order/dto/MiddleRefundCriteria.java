package com.pousheng.middle.order.dto;

import io.terminus.parana.order.dto.RefundCriteria;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/8/10
 */
@Data
public class MiddleRefundCriteria extends RefundCriteria implements Serializable{

    private static final long serialVersionUID = -848661870245726484L;

    //售后单类型
    private Integer refundType;


    //排除的售后单类型
    private Integer excludeRefundType;
}
