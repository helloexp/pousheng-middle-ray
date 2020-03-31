package com.pousheng.middle.shop.enums;


import lombok.ToString;

import java.util.Objects;

/**
 * Description: 门店类型 1：综合门店、2：接单门店、3：下单门店
 * User: liangyj
 * Date: 2018/5/11
 */
@ToString(callSuper = true, includeFieldNames = true)
public enum ShopType{
    GENERAL_SHOP(1,"综合门店"),
    RECEIVING_SHOP(2,"接单门店"),
    ORDERS_SHOP(3,"下单门店");

    private final int value;
    private final String desc;

    ShopType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ShopType from(int value) {
        for (ShopType source : ShopType.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }
    
    public int value() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    public static String fromValue(int value) {
        for (ShopType source : ShopType.values()) {
            if (Objects.equals(source.value, value)) {
                return source.desc;
            }
        }
        return null;
    }

}
