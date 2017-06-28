package com.pousheng.middle.order.enums;


import java.util.Objects;

/**
 * 逆向订单类型
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 12:01 PM
 */
public enum MiddleRefundType {

    ON_SALES_REFUND(1,"售中仅退款"),
    AFTER_SALES_REFUND(2,"售后仅退款"),
    AFTER_SALES_RETURN(3,"售后退货"),
    AFTER_SALES_CHANGE(4,"售后换货");

    private final int value;
    private final String desc;

    MiddleRefundType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static MiddleRefundType from(int value) {
        for (MiddleRefundType source : MiddleRefundType.values()) {
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
