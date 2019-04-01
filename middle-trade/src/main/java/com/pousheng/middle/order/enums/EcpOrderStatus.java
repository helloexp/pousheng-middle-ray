/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 同步电商状态
 */
public enum EcpOrderStatus {

    WAIT_SHIP(0),    //待发货
    SHIPPED_WAIT_SYNC_ECP(1),      //有订单发货
    SYNC_ECP_ING(2),    //同步电商平台中（此状态前端可不用关心，只是为了后端flow通顺）
    SYNC_ECP_SUCCESS_WAIT_RECEIVED(3), //待收货
    CONFIRMED(4),         //确认收货，已完成
    SYNC_ECP_FAIL(-1);   //同步电商失败

    private final int value;

    EcpOrderStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static EcpOrderStatus fromInt(int value){
        for (EcpOrderStatus orderStatus : EcpOrderStatus.values()) {
            if(Objects.equal(orderStatus.value, value)){
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown ecp order status: "+value);
    }
}
