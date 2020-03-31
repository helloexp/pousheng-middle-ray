package com.pousheng.middle.order.enums;

import java.util.Objects;

/**
 * @author bernie
 * @date 2019/6/11
 * 逆向物流类型
 */
public enum ReverseLogisticsTypeEnum {

    HEADLESS("无头件"),
    EXPRESS("物流信息"),
    INSTORE("退货入库单");
    String desc;

    ReverseLogisticsTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static ReverseLogisticsTypeEnum fromName(String name){
        for(ReverseLogisticsTypeEnum typeEnum:ReverseLogisticsTypeEnum.values()){
            if(Objects.equals(typeEnum.name(),name)){
                return typeEnum;
            }
        }
        return null;
    }
}
