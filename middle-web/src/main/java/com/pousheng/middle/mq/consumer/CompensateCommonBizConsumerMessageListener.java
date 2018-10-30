package com.pousheng.middle.mq.consumer;

import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.starter.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.starter.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


/**
 * 通用业务处理，16个线程
 * CompenSateBiz mq处理
 */
@ConditionalOnProperty(name = "biz.common.topic.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@Service
@RocketMQMessageListener(topic = MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC,
        consumerGroup = MqConstants.POUSHENG_MIDDLE_MQ_COMMON_CONSUMER_GROUP,consumeThreadMax = 16)
public class CompensateCommonBizConsumerMessageListener implements RocketMQListener<String> {
    @Autowired
    private CompensateBizLogic compensateBizLogic;

    @Override
    public void onMessage(String message) {
        if (log.isDebugEnabled()){
            log.debug("CompensateCommonBizConsumerMessageListener onMessage,message {}",message);
        }
        compensateBizLogic.consumeMqMessage(message);
    }
}
