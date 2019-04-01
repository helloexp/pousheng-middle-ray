package com.pousheng.middle.mq.producer;

/**
 * RocketMQ消息延迟级别
 * 实际延迟时间按最终rocketMQ的配置为准
 * @author xialongxiang
 */
public enum MessageLevel {
    /**
     * 第1级别，默认：1s，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_1(1),

    /**
     * 第2级别，默认：5s，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_2(2),

    /**
     * 第3级别，默认：10s，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_3(3),

    /**
     * 第4级别，默认：30s，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_4(4),

    /**
     * 第5级别，默认：1m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_5(5),

    /**
     * 第6级别，默认：2m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_6(6),

    /**
     * 第7级别，默认：3m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_7(7),

    /**
     * 第8级别，默认：4m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_8(8),

    /**
     * 第9级别，默认：5m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_9(9),

    /**
     * 第10级别，默认：6m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_10(10),

    /**
     * 第11级别，默认：7m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_11(11),

    /**
     * 第12级别，默认：8m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_12(12),

    /**
     * 第13级别，默认：9m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_13(13),

    /**
     * 第14级别，默认：10m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_14(14),

    /**
     * 第15级别，默认：20m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_15(15),

    /**
     * 第16级别，默认：30m，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_16(16),

    /**
     * 第17级别，默认：1h，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_17(17),

    /**
     * 第18级别，默认：2h，实际延迟时间按最终rocketMQ的配置为准
     */
    LEVEL_18(18);

    /**
     * 延迟级别
     */
    private final int level;

    MessageLevel(int level) {
        this.level = level;
    }

    public int resolve() {
        return this.level;
    }
}
