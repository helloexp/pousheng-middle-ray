package com.pousheng.middle.web.middleLog.dto;

import com.pousheng.middle.item.enums.AttributeEnum;

import java.util.Objects;

/**
 * @author zhaoxw
 * @date 2018/8/17
 */
public enum StockLogTypeEnum {

    HKTOMIDDLE(0, "恒康同步中台"),

    MIDDLETOSHOP(1, "中台同步电商"),

    TRADE(2, "库存交易变更");

    private final Integer value;

    private final String desc;

    StockLogTypeEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据值构造enum
     *
     * @param value key的值
     * @return
     */
    public static StockLogTypeEnum from(Integer value) {
        for (StockLogTypeEnum source : StockLogTypeEnum.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }

    public Integer value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }

}
