package com.pousheng.middle.item.enums;

import java.util.Objects;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */
public enum PsItemGroupSkuMark {

    AUTO(0, "自动打标"),
    ARTIFICIAL(1, "人工打标");

    private final Integer value;
    private final String desc;

    PsItemGroupSkuMark(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static PsItemGroupSkuMark from(Integer value) {
        for (PsItemGroupSkuMark source : PsItemGroupSkuMark.values()) {
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
