package com.pousheng.middle.mq.constant;

/**
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.mq.constant
 * 2018/10/28 16:25
 * pousheng-middle
 */
public interface MqConstants {

    public static final String POUSHENG_MIDDLE_MQ_COMMON_CONSUMER_GROUP = "pousheng-middle-biz-common-consumer-group";

    public static final String POUSHENG_MIDDLE_MQ_EXPORT_CONSUMER_GROUP = "pousheng-middle-biz-export-consumer-group";

    public static final String POUSHENG_MIDDLE_ORDER_FETCH_CONSUMER_GROUP = "pousheng-middle-order-fetch-consumer-group";
    /**
     * CompensateBiz(通用)业务处理topic
     */
    public static final String POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC = "pousheng-middle-common-compensate-biz-topic";

    /**
     * CompensateBiz(导出)业务处理topic
     */
    public static final String POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC = "pousheng-middle-export-compensate-biz-topic";

    public static final String POUSHENG_MIDDLE_ORDER_FETCH_TOPIC = "pousheng-middle-order-fetch-topic";
}
