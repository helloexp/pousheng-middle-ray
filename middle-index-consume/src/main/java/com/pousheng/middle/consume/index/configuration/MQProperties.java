package com.pousheng.middle.consume.index.configuration;

import lombok.Data;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-16 16:41<br/>
 */
@Data
@ConfigurationProperties(prefix = "mq")
public class MQProperties {
    private String topic;
    private String consumeGroup;
    private String nameServerAddr;
    private ConsumeFromWhere consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
    private MessageModel messageModel = MessageModel.CLUSTERING;
    private int consumeThreadMin = 20;
    private int consumeThreadMax = 64;
    private int pullThresholdForQueue = 1000;
}
