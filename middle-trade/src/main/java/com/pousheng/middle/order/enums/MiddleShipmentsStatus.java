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

    WAIT_SYNC_HK(1),  //待同步恒康（mpos）
    SYNC_HK_ING(2),    //同步恒康（mpos）中（此状态前端可不用关心，只是为了后端flow通顺）
    ACCEPTED(3),//已受理
    WAIT_SHIP(4),    //同步成功，待发货
    SHIPPED(5),      //已发货
    CONFIRMD_SUCCESS(6),     //确认收货成功
    WAIT_MPOS_RECEIVE(7),   //待接单
    SYNC_HK_ACCEPT_FAILED(-1),//发货单恒康（mpos）受理失败
    SYNC_HK_FAIL(-2),   //同步恒康失败
    SYNC_HK_CANCEL_ING(-3),       //同步恒康（mpos）取消中（此状态前端可不用关心，只是为了后端flow通顺）
    SYNC_HK_CANCEL_FAIL(-4),   //同步恒康（mpos）失败
    CANCELED(-5),  // 已取消取消
    CONFIRMED_FAIL(-6), //恒康确认收货失败
    REJECTED(-7); //mpos拒单




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
