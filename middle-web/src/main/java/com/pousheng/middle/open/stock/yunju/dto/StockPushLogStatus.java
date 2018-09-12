package com.pousheng.middle.open.stock.yunju.dto;

import java.util.Objects;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/7/23
 */
public enum StockPushLogStatus {

        PUSH_SUCESS(1, "推送成功"),

        PUSH_FAIL(2, "推送失败"),

        DEAL_SUCESS(3, "处理成功"),

        DEAL_FAIL(4, "处理失败");


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
