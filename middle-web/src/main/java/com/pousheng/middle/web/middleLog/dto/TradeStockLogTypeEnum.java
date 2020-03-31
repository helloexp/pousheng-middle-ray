package com.pousheng.middle.web.middleLog.dto;

/**
 * 交易造成的库存变更类型
 *
 * @author zhaoxw
 * @date 2018/8/17
 */
public enum TradeStockLogTypeEnum {

    DEFAULT(100, "DEFAULT"),
    WITHHOLD(101, "WITHHOLD"),
    OCCUPY(102, "OCCUPY"),
    CONFIRM(103, "CONFIRM"),
    CANCEL(104, "CANCEL");

    private Integer code;
    private String desc;

    private TradeStockLogTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }

}
