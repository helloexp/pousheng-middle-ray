package com.pousheng.middle.item.enums;

import java.util.Objects;

/**
 * @author zhaoxw
 * @date 2018/7/10
 */
public enum  PsItemGroupType {

    ALL(0,"全国销售"),

    COMPANY(1,"同公司销售");

    private final Integer value;
    private final String desc;

    PsItemGroupType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static PsItemGroupType from(Integer value) {
        for (PsItemGroupType source : PsItemGroupType.values()) {
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
