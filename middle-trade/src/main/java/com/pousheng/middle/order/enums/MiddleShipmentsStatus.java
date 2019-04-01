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

    WAIT_SYNC_HK(1, "待同步订单派发中心/MPOS"),  //待同步恒康（mpos）
    SYNC_HK_ING(2, "同步订单派发中心/MPOS中"),    //同步恒康（mpos）中（此状态前端可不用关心，只是为了后端flow通顺）
    ACCEPTED(3, "已受理"),//已受理
    WAIT_SHIP(4, "同步成功，待发货"),    //同步成功，待发货
    SHIPPED(5, "已发货"),      //已发货
    CONFIRMD_SUCCESS(6, "订单派发中心确认收货"),     //确认收货成功
    WAIT_MPOS_RECEIVE(7, "待接单"),   //待接单
    SKX_FREEZE(8,"已挂起"),//已挂起
    SYNC_HK_ACCEPT_FAILED(-1,"订单派发中心/MPOS受理失败"),//发货单恒康（mpos）受理失败
    SYNC_HK_FAIL(-2, "同步订单派发中心/MPOS失败"),   //同步恒康失败
    SYNC_HK_CANCEL_ING(-3, "同步订单派发中心取消中"),       //同步恒康（mpos）取消中（此状态前端可不用关心，只是为了后端flow通顺）
    SYNC_HK_CANCEL_FAIL(-4, "取消同步订单派发中心失败"),   //同步恒康（mpos）失败
    CANCELED(-5, "已取消"),  // 已取消取消
    CONFIRMED_FAIL(-6, "订单派发中心确认失败"), //恒康确认收货失败
    REJECTED(-7, "已拒绝"), //mpos拒单
    PART_SHIPPED(10, "部分发货"); //部分发货，不会存储在数据库中，仅作为前端搜索用




    private final int value;
    private final String name;

    MiddleShipmentsStatus(int value ,String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
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
