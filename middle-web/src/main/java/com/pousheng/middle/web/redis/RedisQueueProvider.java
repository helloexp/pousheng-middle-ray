package com.pousheng.middle.web.redis;

import com.google.common.base.Throwables;
import io.terminus.common.redis.utils.JedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;


/**
 * 生产者
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 5/9/16
 * Time: 6:11 PM
 */
@Slf4j
@Component
public class RedisQueueProvider {

    @Autowired
    private JedisTemplate jedisTemplate;

    /**
     * 生产者/消费者模式
     */
    public void startProvider(final String content) {
        log.info("start provider stock info :{}",content);
        try {
            jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
                @Override
                public void action(Jedis jedis) {
                    jedis.lpush(RedisConstants.PUSH_STOCK_LOG_KEY, content);
                }
            });
        } catch (Exception e) {
            log.error("publish stock info:{} fail,cause: {}",content, Throwables.getStackTraceAsString(e));
        }
    }
}
