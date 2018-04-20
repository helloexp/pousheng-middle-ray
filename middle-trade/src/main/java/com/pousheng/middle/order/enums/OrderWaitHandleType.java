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
    HANDLE_DONE(6,"已经处理"),
    DISPATCH_ORDER_FAIL(7,"派单失败，请联系开发人员协助排查"),
    WAREHOUSE_SATE_STOCK_NOT_FIND(8,"存在安全库存没有设置的仓库"),
    ADDRESS_GPS_NOT_FOUND(9,"门店或仓库地址信息不存在"),
    FIND_ADDRESS_GPS_FAIL(10,"门店或仓库地址信息查询失败"),
    WAREHOUSE_STOCK_LOCK_FAIL(11,"mpos仓库商品库存锁定失败"),
    SHOP_STOCK_LOCK_FAIL(12,"mpos门店商品库存锁定失败"),
    WAREHOUSE_RULE_NOT_FOUND(13,"仓库规则不存在"),
    UNKNOWN_ERROR(14,"未知错误");

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
