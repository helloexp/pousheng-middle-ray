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
    WAIT_ALL_HANDLE_DONE(2),  //处理中，待全部处理完成
    WAIT_SHIP(3),    //待发货
    SHIPPED(4),      //待收货（所有发货单全部发货完成）
    CONFIRMED(5),         //确认收货，已完成
    REFUND_APPLY_WAIT_SYNC_HK(-1),    //售中申请退款商家同意待同步恒康取消
    REFUND_SYNC_HK_SUCCESS(-2),      //同步恒康取消成功待退款
    REFUND(-3),                  //已退款
    CANCEL(-4),                  //已取消
    CANCEL_ING(-5),              //取消中(后台流转使用,前台不需要关心)
    CANCEL_FAILED(-6),           //取消失败
    REVOKE_ING(-7),              //撤销中(后台流转使用,前台不需要关心)
    REVOKE_FAILED(-8),           //撤销失败
    CANCEL_SKU_ING(-9),          //子单撤销中(后台流转使用,前台不需要关心)
    CANCEL_SKU_FAILED(-10);       //子单撤销失败(该状态给总单使用,标记时可以调用撤销子单的接口)
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
