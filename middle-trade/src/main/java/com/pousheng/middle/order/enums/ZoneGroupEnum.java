package com.pousheng.middle.order.enums;

import lombok.Getter;

import java.util.stream.Stream;

@Getter
public enum ZoneGroupEnum {

    PROJECT_MANAGER(1, "项目负责人"),
    SUPPORT_MANAGE(2, "支持负责人"),;
    private int code;
    private String desc;

    ZoneGroupEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static boolean contains(int code) {

        return Stream.of(ZoneGroupEnum.values()).anyMatch(x -> x.code == code);

    }
}
