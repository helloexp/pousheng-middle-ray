package com.pousheng.middle.shop.enums;


import lombok.ToString;

import java.util.Objects;

/**
 * Description: 门店状态：1：开启，2：关闭（停用）
 */
@ToString(callSuper = true, includeFieldNames = true)
public enum ShopEnableStatus {
    ENABLE_SHOP(1,"开启"),
    DISABLE_SHOP(-2,"关闭");
    private final int value;
    private final String desc;
    
    ShopEnableStatus(int value,String desc){
        this.value = value;
        this.desc = desc;
    }
    
    public int value(){return value;}
    
    public static String fromValue(int value){
        for(ShopEnableStatus source : ShopEnableStatus.values()){
            if(Objects.equals(source.value,value)){
                return source.desc;
            }
        }        
        return null;
    }
    
}
