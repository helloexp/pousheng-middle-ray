package com.pousheng.middle.order.dto;

import com.pousheng.middle.order.model.RefundWarehouseRules;
import lombok.Data;

import java.io.Serializable;

/**
 * Author: wenchao.he
 * Desc:
 * Date: 2019/8/29
 */
@Data
public class MiddleRefundWarehouseRuleDto extends RefundWarehouseRules implements Serializable {

    private static final long serialVersionUID = -8291269816692950034L;
        
    private String refundWarehouseName;
}
