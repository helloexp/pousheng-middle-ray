package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * @author bernie
 * @date 2019/6/10
 */
public enum ReverseInstoreStatusEnum {

    NEW("新建"),
    RECEIVING("收货中"),
    ACCEPT("已接收"),
    ACCOMPLISH("已结");

    ReverseInstoreStatusEnum(String desc)
    {
        this.desc=desc;
    }
    private String desc;

    public static ReverseInstoreStatusEnum fromDesc(String desc) {
        for (ReverseInstoreStatusEnum status : ReverseInstoreStatusEnum.values()) {
            if (Objects.equal(status.getDesc(), desc)) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown instore status : " + desc);
    }

    public static ReverseInstoreStatusEnum fromName(String name) {
        for (ReverseInstoreStatusEnum status : ReverseInstoreStatusEnum.values()) {
            if (Objects.equal(status.name(), name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown instore name : " + name);
    }

    public String getDesc() {
        return desc;
    }
}
