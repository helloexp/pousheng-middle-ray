/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.order.dto.fsm;

/**
 * 订单类型
 * @author tanlongjun
 */
public enum MiddleOrderType {

    /**
     * 云聚BBC
     */
    BBC(1),
    /**
     * 云聚JIT拣货单
     */
    JIT(2),
    /**
     * 云聚jit时效单
     */
    JIT_REAL_TIME(3),

    /**
     * 补差邮费订单
     */
    POSTAGE(4);

    private final int value;
    MiddleOrderType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
