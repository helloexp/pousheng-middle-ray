package com.pousheng.middle.item.enums;

import java.util.Objects;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */
public enum AttributeEnum {

    DAYS("days", "上市时间"),

    BRAND("brand", "品牌"),

    YEAR("year", "年份"),

    SEASON("season", "季节"),

    STYLE("style", "款型"),

    SERIES("series", "系列"),

    CATEGORY("category", "类别"),

    SEX("sex", "性别");

    private final String value;
    private final String desc;

    AttributeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据值构造enum
     * @param value key的值
     * @return
     */
    public static AttributeEnum from(String value) {
        for (AttributeEnum source : AttributeEnum.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }


}
