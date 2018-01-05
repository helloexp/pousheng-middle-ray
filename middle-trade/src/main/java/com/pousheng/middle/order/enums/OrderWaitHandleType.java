package com.pousheng.middle.order.enums;


import java.util.Objects;

/**
 *
 * @author tony
 */
public enum OrderWaitHandleType {

    WAIT_HANDLE(1,"尚未尝试自动生成发货单"),
    ORDER_HAS_NOTE(2,"订单有备注"),
    JD_PAY_ON_CASH(3,"京东货到付款"),
    SKU_NOT_MATCH(4,"商品对应失败"),
    STOCK_NOT_ENOUGH(5,"库存不足"),
    HANDLE_DONE(6,"已经处理");

    private final int value;
    private final String desc;

    OrderWaitHandleType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    public static OrderWaitHandleType from(int value) {
        for (OrderWaitHandleType source : OrderWaitHandleType.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }
    public int value() {
        return value;
    }
    public String getDesc(){return desc;}
    @Override
    public String toString() {
        return desc;
    }
}
