package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 中台支付类型
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
public enum MiddlePayType {
    ONLINE_PAY(1), //在线支付
    CASH_ON_DELIVERY(2); //货到付款
    private final int value;

    MiddlePayType(int value){
        this.value = value;
    }
    public int getValue(){return value;}

    public static MiddlePayType fromInt(int value){
        for (MiddlePayType payType : MiddlePayType.values()) {
            if(Objects.equal(payType.value, value)){
                return payType;
            }
        }
        throw new IllegalArgumentException("unknown payment type: "+value);
    }
}
