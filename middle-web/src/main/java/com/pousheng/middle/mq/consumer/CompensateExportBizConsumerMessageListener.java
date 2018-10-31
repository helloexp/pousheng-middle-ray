package com.pousheng.middle.mq.consumer;

import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import io.terminus.common.rocketmq.annotation.ConsumeMode;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.annotation.MQSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


/**
 * 通用业务处理，16个线程
 * CompenSateBiz mq处理
 * @author tony
 */
@ConditionalOnProperty(name = "biz.export.topic.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@Service
@MQConsumer
public class CompensateExportBizConsumerMessageListener{

    @Autowired
    private CompensateBizLogic compensateBizLogic;

    @MQSubscribe(topic = MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC, consumerGroup =  MqConstants.POUSHENG_MIDDLE_MQ_EXPORT_CONSUMER_GROUP,
            consumeMode = ConsumeMode.CONCURRENTLY)
    public void onMessage(String message) {

        if (log.isDebugEnabled()){
            log.debug("CompensateExportBizConsumerMessageListener onMessage,message {}",message);
        }

        compensateBizLogic.consumeMqMessage(message);

    }
}
