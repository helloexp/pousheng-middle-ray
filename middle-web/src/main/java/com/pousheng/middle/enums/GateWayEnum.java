package com.pousheng.middle.enums;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/7下午6:24
 */
public enum GateWayEnum {

    JD_GATEWAY("jingdong", "https://api.jd.com/routerjson"),
    OFFICIAL_GATEWAY("official", "http://api-admin.pousheng.com/api/gateway"),
    SUNING_GATEWAY("suning", "https://open.suning.com/api/http/sopRequest"),
    SUNING_SALE_GATEWAY("suning-sale", "https://open.suning.com/api/http/sopRequest"),
    TAOBAO_GATEWAY("taobao", "http://gw.api.taobao.com/router/rest"),
    YJ_GATEWAY("yunjubbc", "xxxx");

    @Getter
    @Setter
    private String channel;

    @Getter
    @Setter
    private String gateway;

    GateWayEnum(String channel, String gateway) {
        this.channel = channel;
        this.gateway = gateway;
    }

    public static GateWayEnum get (String channel) {
        for (GateWayEnum source : GateWayEnum.values()) {
            if (Objects.equals(source.channel, channel)) {
                return source;
            }
        }
        return null;
    }
}
