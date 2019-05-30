package com.pousheng.middle.consume.index;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.consume.index.configuration.MQProperties;
import com.pousheng.middle.consume.index.processor.core.IndexEventListener;
import com.pousheng.middle.consume.index.processor.core.IndexEventProcessor;
import com.pousheng.middle.consume.index.processor.core.IndexEventProcessorWrap;
import com.pousheng.middle.consume.index.processor.core.Processor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-16 14:04<br/>
 */
@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties({
        MQProperties.class,
})
public class IndexConsumerConfiguration implements ApplicationContextAware {
    @Autowired
    private MQProperties mqProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, IndexEventProcessor> beans = applicationContext.getBeansOfType(IndexEventProcessor.class);
        List<IndexEventProcessorWrap> processorWraps = Lists.newArrayList();
        for (Object value : beans.values()) {
            IndexEventProcessor bean = (IndexEventProcessor) value;
            Processor processor = bean.getClass().getAnnotation(Processor.class);
            String table = StringUtils.defaultIfEmpty(processor.value(), processor.table());
            processorWraps.add(new IndexEventProcessorWrap(table, processor.task(), bean));
        }

        IndexEventListener eventListener = new IndexEventListener(processorWraps);
        startConsumer(eventListener);
    }

    private void startConsumer(IndexEventListener eventListener) {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(mqProperties.getConsumeGroup());
        consumer.setNamesrvAddr(mqProperties.getNameServerAddr());
        consumer.setConsumeFromWhere(mqProperties.getConsumeFromWhere());
        consumer.setMessageModel(mqProperties.getMessageModel());
        consumer.setConsumeThreadMax(mqProperties.getConsumeThreadMax());
        consumer.setConsumeThreadMin(mqProperties.getConsumeThreadMin());
        consumer.setPullThresholdForQueue(mqProperties.getPullThresholdForQueue());
        consumer.registerMessageListener(eventListener);

        try {
            consumer.subscribe(mqProperties.getTopic(), "*");
            consumer.start();
            log.info("RocketMQ消费者注册成功 nameSrvAddress={}, group={}", mqProperties.getNameServerAddr(), mqProperties.getConsumeGroup());
        } catch (MQClientException e) {
            log.error("RocketMQ 启动失败，{}", Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }
}
