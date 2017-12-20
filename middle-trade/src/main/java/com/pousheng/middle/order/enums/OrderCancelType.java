package com.pousheng.middle.order.enums;

import java.util.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/7
 * pousheng-middle
 * @author tony
 */
public enum OrderCancelType {
    STOCK_NOT_ENOUGH(1,"同步电商取消"),
    HANDLE_DONE(2,"中台客服取消");

    private final int value;
    private final String desc;

    OrderCancelType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static OrderCancelType from(int value) {
        for (OrderCancelType source : OrderCancelType.values()) {
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
