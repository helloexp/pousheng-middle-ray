package com.pousheng.middle.warehouse.enums;


import java.util.Objects;

/**
 * 订单来源
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 12:01 PM
 */
public enum WarehouseType {

    TOTAL_WAREHOUSE(0,"总仓"),
    SHOP_WAREHOUSE(1,"店仓");

    private final int value;
    private final String desc;

    WarehouseType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static WarehouseType from(int value) {
        for (WarehouseType source : WarehouseType.values()) {
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
