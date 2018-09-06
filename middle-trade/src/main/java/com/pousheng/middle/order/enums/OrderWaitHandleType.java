package com.pousheng.middle.order.enums;


import java.util.Objects;

/**
 * @author tony
 */
public enum OrderWaitHandleType {
    /**
     * 详细备注
     */
    ORIGIN_STATUS_SAVE(-1, "初始状态"),
    WAIT_AUTO_CREATE_SHIPMENT(0, "防止并发"),
    WAIT_HANDLE(1, "尚未尝试自动生成发货单"),
    ORDER_HAS_NOTE(2, "订单有备注"),
    JD_PAY_ON_CASH(3, "京东货到付款"),
    SKU_NOT_MATCH(4, "商品对应失败"),
    STOCK_NOT_ENOUGH(5, "库存不足"),
    HANDLE_DONE(6, "已经处理"),
    DISPATCH_ORDER_FAIL(7, "派单失败，请联系开发人员协助排查"),
    WAREHOUSE_SATE_STOCK_NOT_FIND(8, "存在安全库存没有设置的仓库"),
    ADDRESS_GPS_NOT_FOUND(9, "门店或仓库地址信息不存在"),
    FIND_ADDRESS_GPS_FAIL(10, "门店或仓库地址信息查询失败"),
    WAREHOUSE_STOCK_LOCK_FAIL(11, "mpos仓库商品库存锁定失败"),
    SHOP_STOCK_LOCK_FAIL(12, "mpos门店商品库存锁定失败"),
    WAREHOUSE_RULE_NOT_FOUND(13, "来源店铺没有配置对应的默认发货仓规则"),
    UNKNOWN_ERROR(14, "未知错误"),
    SHOP_MAPPING_MISS(15, "门店对应的店铺映射关系缺失"),
    NOTE_ORDER_NO_SOTCK(17,"备注订单无库存"),
    //有备注的订单正向流转过程:备注订单正在处理->备注订单已经占用库存->确认占库->已经处理
    //有备注订单库存不足:订单有备注->备注订单无库存
    NOTE_ORDER_OCCUPY_SHIPMENT_CREATED(18,"备注订单已经占库"),
    NOTE_ORDER_OCCUPY_SHIPMENT_CANCELED(19,"备注订单客服取消占库发货单");

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

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return desc;
    }
}
