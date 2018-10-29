package com.pousheng.middle.mq.producer;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;

import org.apache.rocketmq.spring.starter.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;

/**
 * @author 赵小涛
 */
@Slf4j
@Service
public class RocketMqProducerService {
    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 立即发送MQ消息
     * @param message messge必须是json字符串
     */
    public void sendMessage(String topic, String message) {
        if (rocketMQTemplate == null) {
            log.error("Error of sending message to MQ: no configure here, message: {}",message);
            return;
        }

        // 开始发送消息
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending message to MQ: {}",message);
            }

            byte[] bytes = message.getBytes(Charset.forName("UTF-8"));

            DefaultMQProducer producer = rocketMQTemplate.getProducer();
            org.apache.rocketmq.common.message.Message msg =
                    new org.apache.rocketmq.common.message.Message(topic, "", bytes);
            //消息异步发送
            SendCallback sendCallback  = new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    //必须打印msgId用来以备查验
                    log.info("Sending message to MQ: {},msgId {}",message,sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("ending message to MQ failed,msg {}",message);
                }
            };
            //消息发送
            producer.send(msg,sendCallback,(long)producer.getSendMsgTimeout());

        }
        catch (Exception ex) {
            log.error("Error of send message to MQ! message: {}, stackTrace: {}",
                    message, Throwables.getStackTraceAsString(ex));
        }
    }

    /**
     * 尝试发送RocketMQ的延迟消息，如果level为空，那么将立即发送消息
     * @param message message必须是json字符串
     * @param level
     */
    public void sendMessage(String topic, String message, MessageLevel level) {
        if (level == null) {
            if (log.isDebugEnabled()) {
                log.debug("No messageLevel, choose normal channel...");
            }

            this.sendMessage(topic, message);
            return;
        }

        if (rocketMQTemplate == null) {
            log.error("Error of sending message to MQ: no configure here, message: {}", message);
            return;
        }

        try {
            long now = System.currentTimeMillis();
            byte[] bytes = message.getBytes(Charset.forName("UTF-8"));

            DefaultMQProducer producer = rocketMQTemplate.getProducer();
            org.apache.rocketmq.common.message.Message msg =
                    new org.apache.rocketmq.common.message.Message(topic, "", bytes);

            // 设置延迟级别
            msg.setDelayTimeLevel(level.resolve());
            //消息异步发送
            SendCallback sendCallback  = new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    //必须打印msgId用来以备查验
                    log.info("Sending message to MQ: {},msgId {}",message,sendResult.getMsgId());
                }
                @Override
                public void onException(Throwable e) {
                    log.error("ending message to MQ failed,msg {}",message);
                }
            };

            producer.send(msg,sendCallback, (long)producer.getSendMsgTimeout());

            long costTime = System.currentTimeMillis() - now;


        } catch (Exception ex) {
            log.error("Error of send message to MQ! message: {}, stackTrace: {}",
                    JSON.toJSON(message), Throwables.getStackTraceAsString(ex));
        }

    }
}
