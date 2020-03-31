package com.pousheng.middle.order.enums;

import java.util.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 */
public enum PoushengGiftOrderRule {
    SATIFIED_FEE_IGINORE_ACTIVITY_ITEM(1,"金额满足但未指定活动商品"),
    SATIFIED_FEE_NOT_IGINORE_ACTIVITY_ITEM(2,"金额满足并且指定活动商品"),
    SATIFIED_QUANTITY_IGINORE_ACTIVITY_ITEM(3,"订单数量满足但未指定活动商品"),
    SATIFIED_QUANTITY_NOT_IGINORE_ACTIVITY_ITEM(4,"订单数量满足并且指定活动商品");
    private final int value;
    private final String desc;

    PoushengGiftOrderRule(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static PoushengGiftOrderRule from(int value) {
        for (PoushengGiftOrderRule source : PoushengGiftOrderRule.values()) {
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
