package com.pousheng.middle.enums;

import lombok.Getter;
import lombok.Setter;

/**
 * Created with IntelliJ IDEA
 * User: lyj
 * Date: 2018/5/30
 * Time: 下午8:25
 */
public enum StockTaskType {
    FULL_TYPE("FULL","全量"),
    INCR_TYPE("INCR","增量");

    @Setter
    @Getter
    private String value;

    @Setter
    @Getter
    private String name;

    StockTaskType(String value, String name) {
        this.value = value;
        this.name = name;
    }
}
