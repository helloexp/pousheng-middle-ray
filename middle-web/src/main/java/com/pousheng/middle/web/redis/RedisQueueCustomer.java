package com.pousheng.middle.web.redis;

import com.google.common.base.Strings;
import io.terminus.common.redis.utils.JedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 5/9/16
 * Time: 6:09 PM
 */
@Slf4j
@Component
public class RedisQueueCustomer {

    @Autowired
    private JedisTemplate jedisTemplate;


    public String pop() {

        //弹出一个元素
        String stockJson = jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.rpop(RedisConstants.PUSH_STOCK_LOG_KEY);
            }
        });

        if (Strings.isNullOrEmpty(stockJson)) {
            return null;
        }
        return stockJson;

    }


}
