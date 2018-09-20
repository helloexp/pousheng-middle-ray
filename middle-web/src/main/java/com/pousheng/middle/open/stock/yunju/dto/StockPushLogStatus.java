package com.pousheng.middle.open.stock.yunju.dto;

import java.util.Objects;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/7/23
 */
public enum StockPushLogStatus {

    DEAL_SUCESS(1, "同步成功"),

    DEAL_FAIL(2, "同步失败"),

    PUSH_SUCESS(3, "推送成功"),

    PUSH_FAIL(4, "推送失败");

    private final int value;
    private final String desc;

        StockPushLogStatus(int value, String desc) {
            this.value = value;
            this.desc = desc;
            }

    /**
     * 根据值构造enum
     * @param value key的值
     * @return
     */
    public static StockPushLogStatus from(int value) {
            for (StockPushLogStatus source : StockPushLogStatus.values()) {
            if (Objects.equals(source.value, value)) {
            return source;
            }
            }
            return null;
            }

    public int value() {
            return value;
            }

    @Override
    public String toString() {
            return desc;
            }

}
