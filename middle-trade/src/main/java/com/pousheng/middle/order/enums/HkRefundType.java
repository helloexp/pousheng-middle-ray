package com.pousheng.middle.order.enums;


import java.util.Objects;

/**
 * 逆向订单类型
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 12:01 PM
 */
public enum HkRefundType {

    HK_AFTER_SALES_RETURN(0,"退货退款"),
    HK_AFTER_SALES_REFUND(1,"仅退款不退货");

    private final int value;
    private final String desc;

    HkRefundType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static HkRefundType from(int value) {
        for (HkRefundType source : HkRefundType.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }
    public int value() {
        return value;
    }
    @Override
    public String toString() {
        return desc;
    }
}
