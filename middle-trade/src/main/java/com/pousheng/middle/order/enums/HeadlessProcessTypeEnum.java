package com.pousheng.middle.order.enums;

import java.util.Objects;

/**
 * @author bernie
 * @date 2019/6/10
 */
public enum HeadlessProcessTypeEnum {

    /**
     * 无头件处理方式
     */
    INIT(0, "初始值"),
    NORMAL(1, "正常匹配"),
    REFUSE(2, "拒收寄回"),
    PROFIT(3, "盘盈");

    private final int value;
    private final String desc;

    HeadlessProcessTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static HeadlessProcessTypeEnum from(int value) {
        for (HeadlessProcessTypeEnum source : HeadlessProcessTypeEnum.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("unknown headless process type value: " + value);
    }

    public static HeadlessProcessTypeEnum from(String name) {
        for (HeadlessProcessTypeEnum source : HeadlessProcessTypeEnum.values()) {
            if (Objects.equals(source.name(), name)) {
                return source;
            }
        }
        throw new IllegalArgumentException("unknown headless process type name: " + name);
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
