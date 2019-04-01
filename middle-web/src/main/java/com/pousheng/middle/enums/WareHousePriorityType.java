package com.pousheng.middle.enums;

import lombok.Getter;
import lombok.Setter;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/5/2
 * Time: 下午8:25
 */
public enum WareHousePriorityType {
    DISTANCE_PRIORITY_TYPE(1,"距离"),
    MANUALLY_PRIORITY_TYP(2,"手动");

    @Setter
    @Getter
    private Integer index;

    @Setter
    @Getter
    private String name;

    WareHousePriorityType(Integer index, String name) {
        this.index = index;
        this.name = name;
    }
}
