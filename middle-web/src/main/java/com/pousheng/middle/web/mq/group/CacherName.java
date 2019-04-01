package com.pousheng.middle.web.mq.group;

import java.util.Objects;

/**
 * @author zhaoxw
 * @date 2018/8/1
 */
public enum  CacherName {

    GROUP_RULE(1,"GROUP_RULE"),

    ITEM_GROUP(2,"ITEM_GROUP");

    private final Integer value;
    private final String desc;

    CacherName(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static CacherName from(Integer value) {
        for (CacherName source : CacherName.values()) {
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
