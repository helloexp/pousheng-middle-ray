package com.pousheng.middle.web.yintai;

import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Map;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/3
 */
public enum YintaiAttributeEnum {

    YEAR("年份"),

    SEASON("季节"),

    COLOR("颜色"),

    CATEGORY("类别"),

    GENDER("性别"),

    SIZE("尺码");

    @Getter
    private final String value;

    YintaiAttributeEnum(String value) {
        this.value = value;
    }

    private static Map<String, YintaiAttributeEnum> cache = Maps.newHashMap();

    static {
        for (YintaiAttributeEnum attributeEnum : YintaiAttributeEnum.values()) {
            cache.put(attributeEnum.value, attributeEnum);
        }
    }

    public static YintaiAttributeEnum from(String value) {
        return cache.get(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
