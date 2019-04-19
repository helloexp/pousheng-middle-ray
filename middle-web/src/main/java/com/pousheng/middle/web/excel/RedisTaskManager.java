package com.pousheng.middle.web.excel;

import com.google.common.base.Throwables;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 14:30<br/>
 */
@Slf4j
@Component
public class RedisTaskManager implements TaskManager {

    private final JedisTemplate jedisTemplate;

    public RedisTaskManager(JedisTemplate jedisTemplate) {
        this.jedisTemplate = jedisTemplate;
    }

    @Override
    public void saveTask(String key, TaskMetaDTO meta) {
        try {
            jedisTemplate.execute(jedis -> {
                jedis.set(key, JsonMapper.nonEmptyMapper().toJson(meta));
            });
        } catch (Exception e){
            log.error("failed to save task {}, {}, cause: {}", key, meta, Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void deleteTask(String key) {
        try {
            jedisTemplate.execute(jedis -> {
                jedis.del(key);
            });
        } catch (Exception e) {
            log.error("failed to delete task {} from redis, cause: {}", key, Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public TaskMetaDTO getTask(String key) {
        try {
            String taskMeta = jedisTemplate.execute(jedis -> {
                return jedis.get(key);
            });
            return JsonMapper.nonEmptyMapper().fromJson(taskMeta, TaskMetaDTO.class);
        } catch (Exception e) {
            log.error("failed to get task {} from redis, cause: {}", key, Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    @Override
    public Set<String> getTasks(String taskType) {
        String type;
        if (StringUtils.hasText(taskType)) {
            type = taskType;
        } else {
            type = "default";
        }

        try {
            return jedisTemplate.execute(jedis -> {
                return jedis.keys("task:" + taskType + ":*");
            });
        } catch (Exception e) {
            log.error("failed to get task keys from redis, cause: {}", Throwables.getStackTraceAsString(e));
            return Collections.emptySet();
        }
    }
}
