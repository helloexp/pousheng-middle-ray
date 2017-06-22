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

    WAIT_SYNC_HK(1),  //待同步恒康
    SYNC_HK_ING(2),    //同步恒康中（此状态前端可不用关心，只是为了后端flow通顺）
    WAIT_SHIP(3),    //同步成功，待发货
    SHIPPED_WAIT_SYNC_ECP(4),      //已发货，待同步电商平台
    SYNC_ECP_ING(5),    //同步电商平台中（此状态前端可不用关心，只是为了后端flow通顺）
    SYNC_ECP_SUCCESS_WAIT_RECEIVED(6),      //待收货
    DONE(7),         //已完成
    SYNC_HK_FAIL(-1),   //同步恒康失败
    SYNC_ECP_FAIL(-2),   //同步电商失败
    BUYER_CANCEL(-3);  // 买家取消订单


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
