package com.pousheng.middle.order.dto.fsm;

import io.terminus.parana.order.dto.fsm.OrderOperation;
import lombok.Getter;

import java.util.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 */
public enum PoushengGiftActivityEvent {
    PUBLISH(1, "publish", "admin"),//发布
    HANDLE(2, "handle", "seller"),//业务处理
    OVER(3, "handle", "admin"),//使其失效
    DELETE(4,"delete","admin");//删除
    @Getter
    private final int value;

    @Getter
    private final String text;

    /**
     * 事件的触发者, 可以有多个角色. 多个角色之间用,分割.
     */
    @Getter
    private final String operator;

    PoushengGiftActivityEvent(int value, String text, String operator) {
        this.value = value;
        this.text = text;
        this.operator = operator;
    }

    public static PoushengGiftActivityEvent fromInt(Integer value) {
        for (PoushengGiftActivityEvent activityEvent : PoushengGiftActivityEvent.values()) {
            if (Objects.equals(activityEvent.getValue(), value)) {
                return activityEvent;
            }
        }
        throw new IllegalArgumentException("unknown activityEvent events: " + value);
    }

    public OrderOperation toOrderOperation() {
        return new OrderOperation(value, text, operator);
    }

}
