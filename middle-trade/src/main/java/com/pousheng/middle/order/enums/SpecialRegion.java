package com.pousheng.middle.order.enums;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/3/6
 * pousheng-middle
 */
public enum SpecialRegion {
    YUANQU(1,"工业园区","园区");
    private final int value;
    private final String name;
    private final String desc;

    SpecialRegion(int value, String name, String desc) {
        this.value = value;
        this.name = name;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
