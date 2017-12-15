package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 地址定位类型
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
public enum AddressBusinessType {
    SHOP(1), //门店
    WAREHOUSE(2);//仓库
    private final int value;

    AddressBusinessType(int value){
        this.value = value;
    }
    public int getValue(){return value;}

    public static AddressBusinessType fromInt(int value){
        for (AddressBusinessType payType : AddressBusinessType.values()) {
            if(Objects.equal(payType.value, value)){
                return payType;
            }
        }
        throw new IllegalArgumentException("unknown invo type: "+value);
    }
}
