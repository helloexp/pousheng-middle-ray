package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 中台电商平台
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
public enum MiddleChannel {
    JD("jingdong", "京东"),
    TAOBAO("taobao", "淘宝"),
    SUNING("suning", "苏宁"),
    FENQILE("fenqile", "分期乐"),
    OFFICIAL("official", "官网"),
    SUNINGSALE("suning-sale", "苏宁特卖"),
    YUNJUBBC("yunjubbc", "云聚"),
    CODOON("codoon","咕咚");

    private final String value;

    private final String desc;

    MiddleChannel(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public String getValue(){return value;}

    public String getDesc(){return desc;}

    public static MiddleChannel from(String value){
        for (MiddleChannel middleChannel : MiddleChannel.values()) {
            if(Objects.equal(middleChannel.value, value)){
                return middleChannel;
            }
        }
        throw new IllegalArgumentException("unknown middle channel type: "+value);
    }
}
