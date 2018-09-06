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
    //SHIP_ADMIN(2, "shipAdmin", "admin"),
    CREATE_SHIPMENT(3, "createShipment", "admin"),
    CONFIRM(4, "confirm", "admin"),
    SYNC_HK(5, "syncHk", "admin"),
    SYNC_ECP(6, "syncEcp", "admin"),
    CANCEL(7, "cancel", "admin"),
    DELETE(8, "delete", "admin"),
    CANCEL_HK(9, "cancelHk", "hk"),
    SYNC_REFUND(10, "syncRefund", "admin"),
    SYNC_SUCCESS(11, "syncSuccess", "hk"),
    SYNC_REFUND_SUCCESS(12, "syncRefundSuccess", "hk"),
    SYNC_RETURN_SUCCESS(13, "syncReturnSuccess", "hk"),
    SYNC_CHANGE_SUCCESS(14, "syncChangeSuccess", "hk"),
    SYNC_CANCEL_SUCCESS(15, "syncCancelSuccess", "hk"),
    SYNC_FAIL(16, "syncFail", "hk"),
    SYNC_CANCEL_FAIL(17, "syncCancelFail", "hk"),
    REFUND_APPLY_AGREE(18, "refundApplyAgree", "seller"),
    REFUND(19, "refund", "admin"),
    HANDLE_DONE(20, "refund", "backend"),
    AUTO_CANCEL_SUCCESS(21,"autoCancelSuccess","seller"),
    AUTO_CANCEL_FAIL(22,"autoCancelFail","seller"),
    REVOKE(23,"revoke","admin"),
    REVOKE_FAIL(24,"revokeFail","seller"),
    REVOKE_SUCCESS(25,"revokeSuccess","seller"),
    CANCEL_SHIP(26,"cancelShip","seller"),
    SYNC_ACCEPT_SUCCESS(27,"syncAcceptSuccess","hk"),
    SYNC_ACCEPT_FAIL(28,"syncAcceptFail","hk"),
    HK_CONFIRMD_SUCCESS(29,"hkConfirmedSuccess","hk"),
    AUTO_HK_CONFIRME_FAILED(30,"AutoHkConfirmedFail","hk"),
    HK_CONFIRME_FAILED(31,"hkConfirmedFail","admin"),
    SYNC_TAOBAO_SUCCESS(32,"syncTaobaoSuccess","hk"),
    SYNC_TAOBAO_FAIL(33,"syncTaobaofail","hk"),
    SYNC_MPOS(50,"syncMpos","admin"),
    SYNC_MPOS_ACCEPT_SUCCESS(51,"syncMposAcceptSuccess","mpos"),
    SYNC_MPOS_ACCEPT_FAIL(52,"syncMposAcceptFail","mpos"),
    MPOS_RECEIVE(53,"mposReceive","mpos"),
    MPOS_REJECT(54,"mposReject","mpos"),
    DISPATCHER(55,"dispatcher","admin"),
    DISPATCHER_FAIL(56,"dispatcherFail","admin"),
    LOST_HANDLE(34,"lostHandle","seller"),
    LOST_CREATE_SHIP(35,"lostCreateShip","seller"),
    LOST_SHIPPED(36,"lostCreateShip","seller"),
    LOST_CONFIRMED(37,"lostConfirmed","seller"),
    SYNC_YYEDI(38, "syncYYEdi", "admin"),
    CANCEL_SHIP_YYEDI(39,"cancelShip","seller"),
    SYNC_SALE_REFUSE_SUCCESS(40,"cancelShip","seller"),
    CONFIRM_OCCUPY_SHIPMENT(41,"confirmOccupyShipment","admin"),
    BUYER_CANCEL(-1, "buyerCancel", "buyer"),
    SELLER_CANCEL(-2, "sellerCancel", "seller,admin"),
    REFUND_APPLY(-3, "refundApply", "buyer"),
    REFUND_APPLY_CANCEL(-5, "refundApplyCancel", "buyer"),
    REFUND_APPLY_REJECT(-6, "refundApplyReject", "seller"),
    RETURN_APPLY(-8, "returnApply", "buyer"),
    RETURN_APPLY_AGREE(-9, "returnApplyAgree", "seller"),
    RETURN_APPLY_CANCEL(-10, "returnApplyCancel", "buyer"),
    RETURN_APPLY_REJECT(-11,"returnApplyReject", "seller"),
    RETURN(-12, "return", "admin"),
    CANCEL_RETURN(-13, "cancelReturn", "admin"),
    RETURN_CHANGE(-14, "returnChange", "hk"),
    RETURN_REJECT(-15, "returnReject", "seller"),
    RETURN_CONFIRM(-16, "returnConfirm", "seller"),
    ON_SALE_RETURN(-17,"onSaleReturn","seller"),
    AFTER_SALE_CANCEL_SHIP(-18,"afterSaleCancelShip","seller"),
    CANCEL_ALL_CHANNEL_SHIPMENT(-19,"cancelAllChannel","seller"),
    AFTER_SALE_CHANGE_RE_CREATE_SHIPMENT(-20,"afterSaleReCreateShipment","admin");


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
