/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 中台订单状态
 * <p>
 * Author:  songrenfei
 * Date: 2017-05-23
 */
public enum MiddleRefundStatus {

    WAIT_HANDLE(1, "待完善"),                                          //待处理
    WAIT_SYNC_HK(2, "待同步恒康(整单)"),                                //待同步恒康(整单)
    SYNC_HK_ING(3, "同步恒康中"),                                       //同步恒康中（此状态前端可不用关心，只是为了后端flow通顺）
    REFUND_SYNC_HK_SUCCESS(4, "同步恒康成功待退款(仅退款)"),              //同步恒康成功待退款(仅退款)
    RETURN_SYNC_HK_SUCCESS(5, "同步恒康成功待退货完成(退货)"),            //同步恒康成功待退货完成(退货)
    CHANGE_SYNC_HK_SUCCESS(6, "同步恒康成功待退货完成(换货)"),            //同步恒康成功待退货完成(换货)
    SYNC_ECP_SUCCESS_WAIT_REFUND(7, "待退款"),                         //待退款
    RETURN_DONE_WAIT_CREATE_SHIPMENT(8, "退货完成待创建发货"),         //退货完成待创建发货
    WAIT_SHIP(9, "待发货"),                                           //待发货
    WAIT_CONFIRM_RECEIVE(10, "待确认收货（所有发货单全部发货完成）"),      //待确认收货（所有发货单全部发货完成）
    REFUND(11, "已退款(仅退款、退货退款)"),                              //已退款(仅退款、退货退款)
    DONE(12, "已完成（换货确认收货）"),                                  //已完成（换货确认收货）
    SYNC_HK_FAIL(-1, "同步恒康失败"),                                  //同步恒康失败
    SYNC_HK_CANCEL_ING(-2, "同步恒康取消中"),                          //同步恒康取消中（此状态前端可不用关心，只是为了后端flow通顺）
    CANCELED(-3, "已取消"),                                           //已取消
    SYNC_HK_CANCEL_FAIL(-4, "同步恒康取消失败"),                        //同步恒康取消失败
    SYNC_ECP_FAIL(-5, "同步电商失败"),                                 //同步电商失败
    DELETED(-6, "已删除");                                            //已删除


    private final int value;
    private final String name;

    MiddleRefundStatus(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static MiddleRefundStatus fromInt(int value) {
        for (MiddleRefundStatus orderStatus : MiddleRefundStatus.values()) {
            if (Objects.equal(orderStatus.value, value)) {
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown order status: " + value);
    }
}
