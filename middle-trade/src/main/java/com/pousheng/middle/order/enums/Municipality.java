package com.pousheng.middle.order.enums;

/**
 *直辖市
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/25
 * pousheng-middle
 */
public enum Municipality {
    SHANGHAI(1,"上海","上海市"),
    BEIJING(2,"北京","北京市"),
    TIANJIN(3,"天津","天津市"),
    CHONGQING(4,"重庆","重庆市");
    private final int value;
    private final String name;
    private final String desc;

    Municipality(int value, String name,String desc) {
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
