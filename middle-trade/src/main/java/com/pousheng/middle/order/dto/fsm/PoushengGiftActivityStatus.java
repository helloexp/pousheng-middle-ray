package com.pousheng.middle.order.dto.fsm;

import com.google.common.base.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 */

public enum  PoushengGiftActivityStatus {
    /**
     * 状态:0.未发布，1.未开始,2.进行中,3.已结束，4.失效
     */
    WAIT_PUBLISH(0,"未发布"),
    WAIT_START(1,"未开始"),
    WAIT_DONE(2,"进行中"),
    DONE(3,"已结束"),
    OVER(-1,"已失效"),
    DELETED(-2,"已删除");
    private final int value;
    private final String name;

    PoushengGiftActivityStatus(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static PoushengGiftActivityStatus fromInt(int value) {
        for (PoushengGiftActivityStatus activityStatus : PoushengGiftActivityStatus.values()) {
            if (Objects.equal(activityStatus.value, value)) {
                return activityStatus;
            }
        }
        throw new IllegalArgumentException("unknown activityStatus status: " + value);
    }
}
