package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 退款单标志
 *
 * @author bernie
 * @date 2019-5-7
 */
public enum MiddleRefundFlagEnum {

    REFUND_SYN_THIRD_PLANT(1, "退货单同步第三方电商失败");

    private final int value;

    MiddleRefundFlagEnum(int value, String desc) {
        this.value = value;
    }

    public int getValue() {return value;}

    public static MiddleRefundFlagEnum fromInt(int value) {
        for (MiddleRefundFlagEnum payType : MiddleRefundFlagEnum.values()) {
            if (Objects.equal(payType.value, value)) {
                return payType;
            }
        }
        throw new IllegalArgumentException("unknown refund flag: " + value);
    }
}
