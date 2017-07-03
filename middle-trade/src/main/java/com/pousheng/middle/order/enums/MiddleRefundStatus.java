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

    WAIT_HANDLE(1),     //待处理
    WAIT_SYNC_HK(2),    //待同步恒康(整单)
    SYNC_HK_ING(3),     //同步恒康中（此状态前端可不用关心，只是为了后端flow通顺）
    REFUND_SYNC_HK_SUCCESS(4),      //同步恒康成功待退款(仅退款)
    RETURN_SYNC_HK_SUCCESS(5),      //同步恒康成功待退货完成(退货)
    CHANGE_SYNC_HK_SUCCESS(5),      //同步恒康成功待退货完成(换货)
    RETURN_DONE_WAIT_REFUND(6),         //退货完成待退款
    RETURN_DONE_WAIT_CREATE_SHIPMENT(7),         //退货完成待创建发货
    WAIT_SHIP(8),         //待发货
    WAIT_CONFIRM_RECEIVE(9),         //待确认收货（所有发货单全部发货完成）
    REFUND(10),              //已退款(仅退款、退货退款)
    DONE(11),              //已完成（换货确认收货）
    SYNC_HK_CANCEL_ING(3),     //同步恒康取消中（此状态前端可不用关心，只是为了后端flow通顺）
    SYNC_HK_FAIL(-1),      //同步恒康失败
    CANCELED(-2),//已取消
    SYNC_HK_CANCEL_FAIL(-2);//同步恒康取消失败



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
