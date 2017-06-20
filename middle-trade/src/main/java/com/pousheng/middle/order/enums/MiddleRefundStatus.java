/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 中台订单状态
 *
 * Author:  songrenfei
 * Date: 2017-05-23
 */
public enum MiddleRefundStatus {

    WAIT_HANDLE(0),  //待处理
    WAIT_SYNC_HK(1),    //售中申请退款商家同意待同步恒康(整单)
    REFUND_SYNC_HK_SUCCESS(2),      //同步恒康成功待退款(仅退款)
    RETURN_SYNC_HK_SUCCESS(3),      //同步恒康成功待退货完成(退货)
    RETURN_DONE(4),         //退货完成待退款
    REFUND(5),              //已退款(仅退款、退货退款)
    SYNC_HK_FAIL(-1);      //同步恒康失败


    private final int value;

    MiddleRefundStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MiddleRefundStatus fromInt(int value){
        for (MiddleRefundStatus orderStatus : MiddleRefundStatus.values()) {
            if(Objects.equal(orderStatus.value, value)){
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown order status: "+value);
    }
}
