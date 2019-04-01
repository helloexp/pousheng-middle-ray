/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.dto.fsm;

import com.google.common.base.Objects;

/**
 * 中台订单状态
 * <p>
 * Author:  songrenfei
 * Date: 2017-05-23
 */
public enum MiddleOrderStatus {

    WAIT_HANDLE(1, "已支付待处理"),                                     //已支付待处理
    WAIT_ALL_HANDLE_DONE(2, "处理中，待全部处理完成"),                   //处理中，待全部处理完成
    WAIT_SHIP(3, "待发货"),                                            //待发货
    SHIPPED(4, "待收货（所有发货单全部发货完成）"),                        //待收货（所有发货单全部发货完成）
    CONFIRMED(5, "确认收货，已完成"),                                   //确认收货，已完成
    REFUND_APPLY_WAIT_SYNC_HK(-1, "售中申请退款商家同意待同步恒康取消"),   //售中申请退款商家同意待同步恒康取消
    REFUND_SYNC_HK_SUCCESS(-2, "同步恒康取消成功待退款"),                //同步恒康取消成功待退款
    REFUND(-3, "已退款"),                                              //已退款
    CANCEL(-4, "已取消"),                                              //已取消

    CANCEL_FAILED(-5, "取消失败"),                                     //取消失败

    REVOKE_FAILED(-6, "撤销失败"),                                    //撤销失败

    JIT_STOCK_RELEASED(-7,"jit时效订单库存已释放");

    private final int value;
    private final String name;

    MiddleOrderStatus(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static MiddleOrderStatus fromInt(int value) {
        for (MiddleOrderStatus orderStatus : MiddleOrderStatus.values()) {
            if (Objects.equal(orderStatus.value, value)) {
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown order status: " + value);
    }
}
