/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.dto.fsm;

import com.google.common.base.Objects;
import io.terminus.parana.order.dto.fsm.OrderStatus;

/**
 * 中台订单状态
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-23
 */
public enum MiddleOrderStatus {

    WAIT_HANDLE(0),  //待处理
    WAIT_SHIP(1),    //待发货
    SHIPPED(2),      //待收货（所有发货单全部发货完成）
    DONE(4),         //已完成
    REFUND_APPLY(-1),    //售中申请退款(整单)
    REFUND_APPLY_AGREED(-2),      //同意售中退款
    REFUND(-3),                  //已退款
    RETURN_APPLY(-7),    //申请退货
    RETURN_APPLY_AGREED(-8),   //同意退款退货
    RETURN(-10),         //买家已退货
    RETURN_CONFIRMED(-11), //商家确认退货
    DELETED(-14);   //订单删除


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
        throw new IllegalArgumentException("unknown status: "+value);
    }
}
