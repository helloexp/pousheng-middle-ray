package com.pousheng.middle.order.enums;


import java.util.Objects;

/**
 * 订单来源
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 12:01 PM
 */
public enum OrderSource {

    POUSHENG(1,"官网"),
    TMALL(2,"天猫"),
    JD(3,"京东"),
    FENQILE(4,"分期乐"),
    SUNING(5,"苏宁");

    private final int value;
    private final String desc;

    OrderSource(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static OrderSource from(int value) {
        for (OrderSource source : OrderSource.values()) {
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
