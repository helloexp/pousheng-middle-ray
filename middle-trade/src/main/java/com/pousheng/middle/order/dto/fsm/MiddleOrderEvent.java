/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.dto.fsm;

import io.terminus.parana.order.dto.fsm.OrderEvent;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import lombok.Getter;

import java.util.Objects;

/**
 * 交易流程中典型的事件
 * <p>
 * Author:  songrenfei
 * Date: 2017-05-24
 */
public enum MiddleOrderEvent {
    HANDLE(1, "handle", "admin"),
    SHIP(2, "ship", "hk"),
    SHIP_ADMIN(2, "shipAdmin", "admin"),
    CREATE_SHIPMENT(2, "createShipment", "admin"),
    CONFIRM(3, "confirm", "admin"),
    SYNC(3, "sync", "admin"),
    SYNC_SUCCESS(3, "syncSuccess", "hk"),
    SYNC_REFUND_SUCCESS(3, "syncRefundSuccess", "hk"),
    SYNC_RETURN_SUCCESS(3, "syncReturnSuccess", "hk"),
    SYNC_FAIL(3, "syncFail", "hk"),
    REFUND_APPLY_AGREE(-4, "refundApplyAgree", "seller"),
    REFUND(-7, "refund", "hk"),
    BUYER_CANCEL(-1, "buyerCancel", "buyer"),
    SELLER_CANCEL(-2, "sellerCancel", "seller,admin"),
    REFUND_APPLY(-3, "refundApply", "buyer"),
    REFUND_APPLY_CANCEL(-5, "refundApplyCancel", "buyer"),
    REFUND_APPLY_REJECT(-6, "refundApplyReject", "seller"),
    RETURN_APPLY(-8, "returnApply", "buyer"),
    RETURN_APPLY_AGREE(-9, "returnApplyAgree", "seller"),
    RETURN_APPLY_CANCEL(-10, "returnApplyCancel", "buyer"),
    RETURN_APPLY_REJECT(-11,"returnApplyReject", "seller"),
    RETURN(-12, "return", "hk"),
    RETURN_CHANGE(-12, "returnChange", "hk"),
    RETURN_REJECT(-13, "returnReject", "seller"),
    RETURN_CONFIRM(-14, "returnConfirm", "seller");

    @Getter
    private final int value;

    @Getter
    private final String text;

    /**
     * 事件的触发者, 可以有多个角色. 多个角色之间用,分割.
     */
    @Getter
    private final String operator;

    MiddleOrderEvent(int value, String text, String operator) {
        this.value = value;
        this.text = text;
        this.operator = operator;
    }

    public static OrderEvent fromInt(Integer value) {
        for (OrderEvent orderEvent : OrderEvent.values()) {
            if (Objects.equals(orderEvent.getValue(), value)) {
                return orderEvent;
            }
        }
        throw new IllegalArgumentException("unknown order events: " + value);
    }

    public OrderOperation toOrderOperation() {
        return new OrderOperation(value, text, operator);
    }

}
