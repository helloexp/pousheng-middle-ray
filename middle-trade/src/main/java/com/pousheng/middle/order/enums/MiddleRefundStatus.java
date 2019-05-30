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
    /**
     *待处理
     */
    WAIT_HANDLE(1, "待完善"),
    /**
     * 待同步恒康(整单)
     */
    WAIT_SYNC_HK(2, "待同步恒康(整单)"),
    /**
     * 同步恒康中（此状态前端可不用关心，只是为了后端flow通顺）
     */
    SYNC_HK_ING(3, "同步恒康中"),
    /**
     * 同步恒康成功待退款(仅退款)
     */
    REFUND_SYNC_HK_SUCCESS(4, "同步恒康成功待退款(仅退款)"),
    /**
     * 同步恒康成功待退货完成(退货)
     */
    RETURN_SYNC_HK_SUCCESS(5, "同步恒康成功待退货完成(退货)"),
    /**
     * 同步恒康成功待退货完成(换货)
     */
    CHANGE_SYNC_HK_SUCCESS(6, "同步恒康成功待退货完成(换货)"),
    /**
     * 待退款
     */
    SYNC_ECP_SUCCESS_WAIT_REFUND(7, "待退款"),
    /**
     * 退货完成待创建发货
     */
    RETURN_DONE_WAIT_CREATE_SHIPMENT(8, "退货完成待创建发货"),
    /**
     * 待发货
     */
    WAIT_SHIP(9, "待发货"),
    /**
     * 待确认收货(所有发货单全部发货完成)
     */
    WAIT_CONFIRM_RECEIVE(10, "待确认收货(所有发货单全部发货完成)"),
    /**
     * 已退款(仅退款、退货退款)
     */
    REFUND(11, "已退款(仅退款、退货退款)"),
    /**
     * 已完成(换货确认收货)
     */
    DONE(12, "已完成(换货确认收货)"),
    /**
     * 丢件补发-待创建发货单
     */
    LOST_WAIT_CREATE_SHIPMENT(13,"丢件补发-待创建发货单"),
    /**
     * 丢件补发-待发货
     */
    LOST_WAIT_SHIP(14,"丢件补发-待发货"),
    /**
     * 丢件补发-已经发货
     */
    LOST_SHIPPED(15,"丢件补发-已经发货"),
    /**
     * 丢件补发-客户确认收货
     */
    LOST_DONE(16,"丢件补发-客户确认收货"),
    /**
     * 同步恒康拒收单成功
     */
    SALE_REFUSE_SUCCESS(17,"同步恒康拒收单成功"),
    /**
     * 退货完成待确认发货
     */
    RETURN_DONE_WAIT_CONFIRM_OCCUPY_SHIPMENT(18,"退货完成待确认发货"),
    /**
     * 部分退货完成待确认发货
     */
    PART_RETURN_DONE_WAIT_CONFIRM_OCCUPY_SHIPMENT(19,"部分退货完成待确认发货"),

    /**
     * 换货 待买家退货
     */
    WAIT_BUYER_RETURN_GOODS(20,"同意换货等待买家退货"),
    /**
     * 换货成功
     */
    EXCHANGE_SUCCESS(21,"换货成功"),
    /**
     * 换货关闭，转退货退款
     */
    WAIT_SELLER_SEND_GOODS(22,"待发出换货商品"),

    /**
     * 换货关闭转退货退款
     */
    EXCHANGE_TO_REFUND(23,"换货关闭转退货退款"),

    /**
     * 同步恒康失败
     */
    SYNC_HK_FAIL(-1, "同步恒康失败"),
    /**
     * 同步恒康取消中
     */
    SYNC_HK_CANCEL_ING(-2, "同步恒康取消中"),
    /**
     * 已取消
     */
    CANCELED(-3, "已取消"),
    /**
     * 同步恒康取消失败
     */
    SYNC_HK_CANCEL_FAIL(-4, "同步恒康取消失败"),
    /**
     * 同步电商失败
     */
    SYNC_ECP_FAIL(-5, "同步电商失败"),
    /**
     * 已删除
     */
    DELETED(-6, "已删除"),
    /**
     * 换货关闭
     */
    EXCHANGE_CLOSED(-7, "换货关闭"),

    REFUND_SYNC_ECP_FAIL(-99, "待退款（退货单同步电商失败)");


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
