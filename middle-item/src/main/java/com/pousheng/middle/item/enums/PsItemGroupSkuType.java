package com.pousheng.middle.item.enums;

import java.util.Objects;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */
public enum PsItemGroupSkuType {

    EXCLUDE(0, "排除商品"),
    GROUP(1, "组内商品");

    private final Integer value;
    private final String desc;

    PsItemGroupSkuType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static PsItemGroupSkuType from(Integer value) {
        for (PsItemGroupSkuType source : PsItemGroupSkuType.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }

    public Integer value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
