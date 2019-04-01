package com.pousheng.middle.web.shop.dto;

import lombok.Data;

/**
 * @author zhaoxw
 * @date 2018/8/7
 */

@Data
public class Channel {

    String name;

    String code;

    public String getName() {
        return name;
    }

    public Channel name(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Channel code(String code) {
        this.code = code;
        return this;
    }
}
