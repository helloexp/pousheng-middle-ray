package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 *恒康的平台类型
 * Created by tony on 2017/7/27.
 * pousheng-middle
 */
public enum HkOnlineType {
    TAOBAO(0), //淘宝
    JINGDONG(4),//京东
    SUNING(6),//苏宁
    POUSHENG(13),//宝胜平台
    FENQILE(21),//分期乐
    OFFICIAL(27),//端点平台
    OTHERS(99);
    private final int value;

    HkOnlineType(int value){
        this.value = value;
    }

    public int getValue(){return value;}

    public static HkOnlineType fromInt(int value){
        for (HkOnlineType onlineType : HkOnlineType.values()) {
            if(Objects.equal(onlineType.value, value)){
                return onlineType;
            }
        }
        throw new IllegalArgumentException("unknown payment type: "+value);
    }
}
