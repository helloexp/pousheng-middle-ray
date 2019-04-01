package com.pousheng.middle.order.enums;


import java.util.Objects;

/**
 * 售后单来源
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 12:01 PM
 */
public enum RefundSource {

    THIRD(1,"第三方同步"),
    MANUAL(2,"手动创建");

    private final int value;
    private final String desc;

    RefundSource(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static RefundSource from(int value) {
        for (RefundSource source : RefundSource.values()) {
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
