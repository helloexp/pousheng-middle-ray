/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.dto.fsm;

import com.google.common.base.Objects;

/**
 * 中台订单状态
 *
 * Author:  songrenfei
 * Date: 2017-05-23
 */
public enum MiddleOrderStatus {

    WAIT_HANDLE(1),  //已支付待处理
    WAIT_SHIP(2),    //待发货
    SHIPPED(3),      //待收货（所有发货单全部发货完成）
    DONE(4),         //已完成
    REFUND_APPLY_WAIT_SYNC_HK(-1),    //售中申请退款商家同意待同步恒康(仅退款)
    REFUND_SYNC_HK_SUCCESS(-2),      //同步恒康成功待退款
    REFUND(-3);                  //已退款



    private final int value;

    MiddleOrderStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MiddleOrderStatus fromInt(int value){
        for (MiddleOrderStatus orderStatus : MiddleOrderStatus.values()) {
            if(Objects.equal(orderStatus.value, value)){
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown order status: "+value);
    }
}
