package com.pousheng.middle.item.enums;

import java.util.Objects;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */
public enum ItemRuleType {

    SHOP(0, "店铺分组"),
    WAREHOUSE(1, "仓库分组");

    private final Integer value;
    private final String desc;

    ItemRuleType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ItemRuleType from(Integer value) {
        for (ItemRuleType source : ItemRuleType.values()) {
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
