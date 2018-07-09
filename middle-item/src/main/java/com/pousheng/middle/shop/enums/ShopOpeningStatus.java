package com.pousheng.middle.shop.enums;


import lombok.ToString;

import java.util.Objects;

/**
 * Description: 营业类型 1：营业，2：歇业
 * User: liangyj
 * Date: 2018/5/11
 */
@ToString(callSuper = true, includeFieldNames = true)
public enum ShopOpeningStatus {

    OPENING(1,"营业"),
    CLOSING(2,"歇业");

    private final int value;
    private final String desc;

    ShopOpeningStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ShopOpeningStatus from(int value) {
        for (ShopOpeningStatus source : ShopOpeningStatus.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }
    public int value() {
        return value;
    }
}
