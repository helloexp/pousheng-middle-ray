package com.pousheng.middle.item.enums;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */
public enum AttributeRelationEnum {

    EXCLUDE(1, "不包含"),
    IN(2, "包含"),
    BETWEEN(3, "区间"),
    AFTER(4, " 大于"),
    BEFORE(5, "小于");


    private final Integer value;
    private final String desc;

    AttributeRelationEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
