package com.pousheng.middle.item.constant;

import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Map;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/17
 */
public enum ItemPushStatus {

    SUCCESS(1, "同步成功"),
    FAIL(-1, "同步失败"),
    ;

    @Getter
    private Integer value;
    @Getter
    private String desc;

    ItemPushStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    private final static Map<Integer, ItemPushStatus> cache = Maps.newHashMapWithExpectedSize(2);

    static {
        for (ItemPushStatus obj : ItemPushStatus.values()) {
            cache.put(obj.value, obj);
        }
    }

    public static ItemPushStatus from(Integer value) {
        return cache.get(value);
    }

}
