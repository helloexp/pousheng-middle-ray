package com.pousheng.middle.item.enums;


import java.util.Objects;

/**
 * 商品类型
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 12:01 PM
 */
public enum PsSpuType {

    POUSHENG(1,"电商"),
    MPOS(2,"MPOS");

    private final int value;
    private final String desc;

    PsSpuType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static PsSpuType from(int value) {
        for (PsSpuType source : PsSpuType.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }
    public int value() {
        return value;
    }
    @Override
    public String toString() {
        return desc;
    }
}
