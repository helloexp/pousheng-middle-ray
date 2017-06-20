/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 中台发货单状态
 *
 * Author:  songrenfei
 * Date: 2017-05-23
 */
public enum MiddleShipmentsStatus {

    WAIT_SYNC_HK(0),  //待同步恒康
    WAIT_SHIP(1),    //待发货
    SHIPPED(2),      //待收货（所有发货单全部发货完成）
    DONE(3),         //已完成
    SYNC_HK_FAIL(-1),   //同步恒康失败
    BUYER_CANCEL(-2);  // 买家取消订单


    private final int value;

    MiddleShipmentsStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MiddleShipmentsStatus fromInt(int value){
        for (MiddleShipmentsStatus orderStatus : MiddleShipmentsStatus.values()) {
            if(Objects.equal(orderStatus.value, value)){
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown shipments status: "+value);
    }
}
