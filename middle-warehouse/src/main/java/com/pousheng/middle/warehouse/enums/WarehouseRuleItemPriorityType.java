package com.pousheng.middle.warehouse.enums;


import java.util.Objects;

/**
 * 仓派单优先级类型
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 12:01 PM
 */
public enum WarehouseRuleItemPriorityType {

    DISTANCE(1,"距离"),
    PRIORITY(2,"优先级");

    private final int value;
    private final String desc;

    WarehouseRuleItemPriorityType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static WarehouseRuleItemPriorityType from(int value) {
        for (WarehouseRuleItemPriorityType source : WarehouseRuleItemPriorityType.values()) {
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
