package com.pousheng.middle.open.stock.yunju;

import com.pousheng.middle.web.middleLog.dto.StockLogTypeEnum;

import java.util.Objects;

public enum YunjuErrorCodeEnum {
    SUCCESS("0","全部成功"),
    PARTIAL_FAILURE("1","部分成功");

    private String value;
    private String desc;

    YunjuErrorCodeEnum(String value, String desc){
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据值构造enum
     *
     * @param value key的值
     * @return
     */
    public static YunjuErrorCodeEnum from(int value) {
        for (YunjuErrorCodeEnum source : YunjuErrorCodeEnum.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }
}
