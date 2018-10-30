package com.pousheng.middle.web.mq.warehouse;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.core.TerminusMQProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * 库存发生变动，推送mq消息
 *
 * @auther feisheng.ch
 * @time 2018/7/18
 */
@Component
@Slf4j
@MQConsumer
public class InventoryChangeProducer {


    @Autowired
    private TerminusMQProducer producer;
    @Autowired
    private JedisTemplate jedisTemplate;

    @Value("${terminus.rocketmq.poushengInventoryTopic}")
    private String poushengInventoryTopic;

    private static final String INVENTORY_CHANGE_SEND_ERROR_KEY = "INVENTORY:CHANGE:SEND:ERROR";

    /**
     * 接收触发可用库存发送变化的skuCode列表，写入队列
     * @param skuCodeList
     * @return
     */
    public Response<Boolean> handleInventoryChange(List<InventoryChangeDTO> skuCodeList) {
        if (ObjectUtils.isEmpty(skuCodeList)) {
            log.error("fail to handle inventory change, because the skuCode is empty");
            return Response.fail("inventory.change.handle.fail");
        }

        try {
            log.info("inventory changed: start to send mq message out");

            List<List<InventoryChangeDTO>> parts = Lists.partition(skuCodeList, 100);
            for (List<InventoryChangeDTO> part : parts) {
                SendResult sendResult = sendData(poushengInventoryTopic, JSON.toJSONString(part), INVENTORY_CHANGE_SEND_ERROR_KEY);
                if (SendStatus.SEND_OK != sendResult.getSendStatus()) {
                    log.error("fail to handle inventory change, because fail msg return from mq: {}", sendResult);
                }

                log.info("inventory changed: success to send mq message part out, msgId:{}", sendResult.getMsgId());
            }
        } catch (Exception e) {
            log.error("fail to handle inventory change, because: {}", Throwables.getStackTraceAsString(e));
            return Response.fail("inventory.change.handle.fail");
        }

        return Response.ok(Boolean.TRUE);
    }

    /**
     * 接收触发可用库存发送变化的skuCode，写入队列
     * @param inventoryChange
     * @return
     */
    public Response<Boolean> handleInventoryChange(InventoryChangeDTO inventoryChange) {
        List<InventoryChangeDTO> skuCodeList = Lists.newArrayList();
        skuCodeList.add(inventoryChange);
        if (ObjectUtils.isEmpty(inventoryChange)) {
            log.error("fail to handle inventory change, because the skuCode is empty");
            return Response.fail("inventory.change.handle.fail");
        }
        try {
            log.info("inventory changed: start to send mq message out");
            SendResult sendResult = sendData(poushengInventoryTopic, JSON.toJSONString(skuCodeList), INVENTORY_CHANGE_SEND_ERROR_KEY);
            if (SendStatus.SEND_OK != sendResult.getSendStatus()) {
                log.error("fail to handle inventory change, because fail msg return from mq: {}", sendResult);
            }
                log.info("inventory changed: success to send mq message part out, msgId:{}", sendResult.getMsgId());
        } catch (Exception e) {
            log.error("fail to handle inventory change, because: {}", Throwables.getStackTraceAsString(e));
            return Response.fail("inventory.change.handle.fail");
        }
        return Response.ok(Boolean.TRUE);
    }


    /**
     * 具备一次重试机制的发送封装
     * @param topic
     * @param data
     * @return
     */
    private SendResult sendData (final String topic, final String data, String redisKey) {
        try {
            SendResult sendResult = producer.send(topic, data);

            if (SendStatus.SEND_OK != sendResult.getSendStatus()) {
                log.info("inventory change send mq fail :{}", JSON.toJSONString(sendResult));
                try {
                    jedisTemplate.execute(jedis -> {
                        jedis.lpush(redisKey, data);
                    });
                } catch (Exception e) {
                    log.error("inventory change send mq error, log to redis fail,cause: {}", data, Throwables.getStackTraceAsString(e));
                }
            }

            return sendResult;
        } catch (Exception e) {
            log.error("fail to send mq message, because: {}", Throwables.getStackTraceAsString(e));

            try {
                jedisTemplate.execute(jedis -> {
                    jedis.lpush(redisKey, data);
                });
            } catch (Exception e2) {
                log.error("send mq error, log to redis fail,cause: {}", data, Throwables.getStackTraceAsString(e));
            }

            throw e;
        }

    }

    /**
     * 定期去重新发送失败任务
     */
    //@Scheduled(cron = "0 0/1 * * * *")
    public void reSend() {
        log.info("START JOB InventoryChangeProducer.reSend");
        // 处理库存变动
        for (int i = 0; i < 50; i++) {
            String invChangeData = jedisTemplate.execute(jedis -> {
                return jedis.rpop(INVENTORY_CHANGE_SEND_ERROR_KEY);
            });

            if (org.springframework.util.StringUtils.isEmpty(invChangeData)) {
                break;
            }

            SendResult sendResult = sendData(poushengInventoryTopic, invChangeData, INVENTORY_CHANGE_SEND_ERROR_KEY);
            if (SendStatus.SEND_OK != sendResult.getSendStatus()) {
                log.error("inventory change mq resend fail,cause: {}", JSON.toJSONString(sendResult));
            }
        }
        log.info("END JOB InventoryChangeProducer.reSend");
    }

}
