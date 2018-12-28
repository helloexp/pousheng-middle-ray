package com.pousheng.middle.mq.producer;

import com.google.common.base.Throwables;
import io.terminus.common.rocketmq.core.TerminusMQProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 赵小涛
 */
@Slf4j
@Service
public class RocketMqProducerService {
    @Autowired
    private TerminusMQProducer producer;

    /**
     * 立即发送MQ消息
     *
     * @param message messge必须是json字符串
     */
    public void sendMessage(String topic, String message) {
        if (producer == null) {
            log.error("Error of sending message to MQ: no configure here, message: {}", message);
            return;
        }

        // 开始发送消息
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending message to MQ: {}", message);
            }

            //消息异步发送
            SendCallback sendCallback = new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    //必须打印msgId用来以备查验
                    log.info("Sending message to MQ: {},msgId {}", message, sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("ending message to MQ failed,msg {}", message);
                }
            };
            //消息发送
            producer.asyncSend(topic, message, sendCallback);

        } catch (Exception ex) {
            log.error("Error of send message to MQ! message: {}, stackTrace: {}",
                    message, Throwables.getStackTraceAsString(ex));
        }
    }

    /**
     * 顺序异步发送消息
     * @param topic
     * @param message
     * @param shardKey
     */
    public void asyncSendOrderly(String topic, String message,Object shardKey,int queueSize) {
        if (producer == null) {
            log.error("Error of sending message to MQ: no configure here, message: {}", message);
            return;
        }

        // 开始发送消息
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending message to MQ: {}", message);
            }

            //消息异步发送
            SendCallback sendCallback = new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    //必须打印msgId用来以备查验
                    log.info("Sending message to MQ: {},msgId {},msgQueueId:{}", message, sendResult.getMsgId(),
                        sendResult.getMessageQueue().getQueueId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("ending message to MQ failed,msg {}", message);
                }
            };
            //消息发送
            producer.asyncSendOrderly(topic,"" ,message, shardKey,sendCallback,queueSize);

        } catch (Exception ex) {
            log.error("Error of send message to MQ! message: {}, stackTrace: {}",
                message, Throwables.getStackTraceAsString(ex));
        }
    }

    /**
     * 异步发送消息到指定队列
     * @param topic
     * @param message
     * @param queueIndex
     */
    public void asyncSendOrderly(String topic, String message,int queueIndex) {
        if (producer == null) {
            log.error("Error of sending message to MQ: no configure here, message: {}", message);
            return;
        }

        // 开始发送消息
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending message to MQ: {}", message);
            }

            //消息异步发送
            SendCallback sendCallback = new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    //必须打印msgId用来以备查验
                    log.info("Sending message to MQ: {},msgId {},msgQueueId:{}", message, sendResult.getMsgId(),
                        sendResult.getMessageQueue().getQueueId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("ending message to MQ failed,msg {}", message);
                }
            };
            //消息发送
            producer.asyncSendToTargetQueue(topic,"", message, queueIndex,sendCallback);

        } catch (Exception ex) {
            log.error("Error of send message to MQ! message: {}, stackTrace: {}",
                message, Throwables.getStackTraceAsString(ex));
        }
    }

}
