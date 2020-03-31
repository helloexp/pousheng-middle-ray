package com.pousheng.middle.enums;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/2下午2:57
 */
public enum OpenShopEnum {
    enable_open_shop_enum(1,"启用"),
    disable_open_shop_enum(-1,"禁用");

    @Getter
    @Setter
    private Integer index;

    @Getter
    @Setter
    private String value;

    OpenShopEnum(Integer index, String value) {
        this.index = index;
        this.value = value;
    }

}
