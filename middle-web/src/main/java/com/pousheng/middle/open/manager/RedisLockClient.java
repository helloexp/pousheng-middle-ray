package com.pousheng.middle.open.manager;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.terminus.common.redis.utils.JedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * redis 锁工具类
 *
 * @author tanlongjun
 */
@Component
@Slf4j
public class RedisLockClient implements InitializingBean {

    /**
     * redis lua锁脚本文件路径
     */
    public static final String REDIS_LOCK_LUA_PATH = "redis_lock.lua";

    /**
     * redis lua解锁脚本路径
     */
    public static final String REDIS_UNLOCK_LUA_PATH = "redis_unlock.lua";

    private String LUA_LOCK_SCRIPT;

    private String LUA_UNLOCK_SCRIPT;

    private String LUA_LOCK_SCRIPT_SHA;

    private String LUA_UNLOCK_SCRIPT_SHA;

    @Autowired
    private JedisTemplate jedisTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        LUA_LOCK_SCRIPT = Resources.toString(Resources.getResource(REDIS_LOCK_LUA_PATH), Charsets.UTF_8);
        if (StringUtils.isNotBlank(LUA_LOCK_SCRIPT)) {

            LUA_LOCK_SCRIPT_SHA = jedisTemplate.execute(jedis -> {
                return jedis.scriptLoad(LUA_LOCK_SCRIPT);
            });
            log.info("redis lock script sha:{}", LUA_LOCK_SCRIPT_SHA);
        }

        LUA_UNLOCK_SCRIPT = Resources.toString(Resources.getResource(REDIS_UNLOCK_LUA_PATH), Charsets.UTF_8);
        if (StringUtils.isNotBlank(LUA_UNLOCK_SCRIPT)) {
            LUA_UNLOCK_SCRIPT_SHA = jedisTemplate.execute(jedis -> {
                return jedis.scriptLoad(LUA_UNLOCK_SCRIPT);
            });
            log.info("redis unlock script sha:{}", LUA_UNLOCK_SCRIPT_SHA);
        }

    }

    /**
     * 加锁
     *
     * @param key
     * @param ttl
     * @param ticket
     * @return
     */
    public boolean lock(String key, String ttl, String ticket) {
        List<String> keys = Lists.newArrayList();
        keys.add(key);
        List<String> args = Lists.newArrayList();
        args.add(ttl);
        args.add(ticket);

        try {
            Object flag = jedisTemplate.execute(jedis -> {
                if (StringUtils.isNotBlank(LUA_LOCK_SCRIPT_SHA)) {
                    return jedis.evalsha(LUA_LOCK_SCRIPT_SHA, keys, args);
                } else {
                    return jedis.eval(LUA_LOCK_SCRIPT, keys, args);
                }
            });
            if (flag == null) {
                return false;
            }
            return "1".equals(flag.toString());
        } catch (Exception e) {
            log.error("failed to get a redis lock.key:{}", key, e);
        }
        return false;
    }

    /**
     * 解锁
     *
     * @param key
     * @param ticket
     */
    public void unlock(String key, String ticket) {
        List<String> keys = Lists.newArrayList();
        keys.add(key);
        List<String> args = Lists.newArrayList();
        args.add(ticket);

        try {
            jedisTemplate.execute(jedis -> {
                if (StringUtils.isNotBlank(LUA_UNLOCK_SCRIPT_SHA)) {
                    jedis.evalsha(LUA_UNLOCK_SCRIPT_SHA, keys, args);
                } else {
                    jedis.eval(LUA_UNLOCK_SCRIPT, keys, args);
                }
            });
        } catch (Exception e) {
            log.error("failed to unlock {}.", key, e);
        }
    }

}
