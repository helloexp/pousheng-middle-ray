package com.pousheng.middle.web.redis;

import io.terminus.common.redis.utils.JedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * Created by songrenfei on 2018/11/22
 */
@Component
@Slf4j
public class ServerSwitchOnOperationLogic {


    @Autowired
    private JedisTemplate jedisTemplate;

    @PostConstruct
    public void init() throws Exception {
        openServer();
    }

    /**
     * 判断server是否开启
     * @return true开始 false关闭
     */
    public Boolean serverIsOpen(){

        String swit = jedisTemplate.execute(jedis -> {
            return jedis.get(RedisConstants.SERVER_SWITCH_ON);
        });
        return !Objects.equals(swit, "off");
    }


    /**
     * 开启服务
     */
    public void openServer(){

        log.info("START-OPEN-SERVER.....");
        jedisTemplate.execute(jedis -> {
            jedis.set(RedisConstants.SERVER_SWITCH_ON, "on");
        });
        log.info("OPEN-SERVER-SUCCESS.....");

    }

    /**
     * 关闭服务
     */
    public void closeServer(){

        log.info("START-CLOSE-SERVER.....");
        jedisTemplate.execute(jedis -> {
            jedis.set(RedisConstants.SERVER_SWITCH_ON, "off");
        });
        log.info("CLOSE-SERVER-SUCCESS.....");


    }
}
