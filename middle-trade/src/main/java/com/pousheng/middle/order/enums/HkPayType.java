package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 *恒康支付类型
 * Created by tony on 2017/7/27.
 * pousheng-middle
 */
public enum HkPayType {
    HK_ONLINE_PAY(0), //在线支付
    HK_CASH_ON_DELIVERY(1); //货到付款
    private final int value;

    HkPayType(int value){
        this.value = value;
    }

    public int getValue(){return value;}

    public static HkPayType fromInt(int value){
        for (HkPayType payType : HkPayType.values()) {
            if(Objects.equal(payType.value, value)){
                return payType;
            }
        }
        throw new IllegalArgumentException("unknown payment type: "+value);
    }
}
