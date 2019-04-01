package com.pousheng.middle.order.enums;

import io.terminus.parana.order.enums.ShipmentType;

import java.util.Objects;

/**
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.order.enums
 * 2018/8/13 11:19
 * pousheng-middle
 */
public enum MiddleShipmentType {
    /**
     *占用库存发货单
     */
    LOST_SHIP(3,"丢件补发发货单");


    private final int value;
    private final String name;

    MiddleShipmentType(int value ,String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static MiddleShipmentType fromInt(int value){
        for (MiddleShipmentType middleShipmentType : MiddleShipmentType.values()) {
            if(com.google.common.base.Objects.equal(middleShipmentType.value, value)){
                return middleShipmentType;
            }
        }
        throw new IllegalArgumentException("unknown shipments status: "+value);
    }

}
