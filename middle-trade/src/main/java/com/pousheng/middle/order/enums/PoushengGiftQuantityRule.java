package com.pousheng.middle.order.enums;

import java.util.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 */
public enum  PoushengGiftQuantityRule {
    NO_LIMIT_PARTICIPANTS(1,"不限定参与人数"),
    LIMIT_PARTICIPANTS(2,"限定参与人数");
    private final int value;
    private final String desc;

    PoushengGiftQuantityRule(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static PoushengGiftQuantityRule from(int value) {
        for (PoushengGiftQuantityRule source : PoushengGiftQuantityRule.values()) {
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
