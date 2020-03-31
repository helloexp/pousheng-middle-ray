package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * @author bernie
 * @date 2019/6/10
 */
public enum ReverseHeadlessStatusEnum {

    RECEIVE_ACCOMPLISH("收货完成"),
    CLOSE("关闭");

    ReverseHeadlessStatusEnum(String desc){
        this.desc=desc;
    }
    private String desc;

    public String getDesc() {
        return desc;
    }

    public static ReverseHeadlessStatusEnum fromDesc(String desc) {
        for (ReverseHeadlessStatusEnum status : ReverseHeadlessStatusEnum.values()) {
            if (Objects.equal(status.getDesc(), desc)) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown headless status : " + desc);
    }

    public static ReverseHeadlessStatusEnum fromName(String name) {
        for (ReverseHeadlessStatusEnum status : ReverseHeadlessStatusEnum.values()) {
            if (Objects.equal(status.name(), name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown headless name : " + name);
    }
}
