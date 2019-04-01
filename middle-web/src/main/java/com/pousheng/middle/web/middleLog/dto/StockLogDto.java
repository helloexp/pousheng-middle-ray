package com.pousheng.middle.web.middleLog.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhaoxw
 * @date 2018/8/17
 */

@Data
public class StockLogDto implements Serializable{

    private static final long serialVersionUID = -7207973527377259619L;

    private String logJson;

    private Integer type;

    public String getLogJson() {
        return logJson;
    }

    public StockLogDto logJson(String logJson) {
        this.logJson = logJson;
        return this;
    }

    public Integer getType() {
        return type;
    }

    public StockLogDto type(Integer type) {
        this.type = type;
        return this;
    }
}
