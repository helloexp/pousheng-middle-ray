package com.pousheng.middle.web.task;

import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

/**
 * Created by songrenfei on 2017/4/14
 */
@Slf4j
@Component
public class SyncParanaTaskRedisHandler {

    @Autowired
    private JedisTemplate jedisTemplate;

    private final static String BASE_KEY = "SYNC_TASK";


    /**
     * 创建Task
     * @param task 任务
     * @return 任务 ID
     */
    public String saveTask(SyncTask task){

        String value = JsonMapper.nonDefaultMapper().toJson(task);
        String key = key();

        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(key, 30*60,value);
            }

        });

        return key;

    }


    /**
     * 更新Task
     * @param key 任务ID
     * @param task 任务
     * @return 任务 ID
     */
    public void updateTask(String key,SyncTask task){

        String value = JsonMapper.nonDefaultMapper().toJson(task);

        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(key, 30*60,value);
            }

        });

    }



    public SyncTask getTask (final String key){
        String value =  jedisTemplate.execute(new JedisTemplate.JedisAction<String>(){
            @Override
            public String action(Jedis jedis) {
                return jedis.get(key);
            }
        });

        return JsonMapper.nonDefaultMapper().fromJson(value,SyncTask.class);
    }




    private String key() {
        return BASE_KEY + ":" + DateTime.now().toDate().getTime();
    }


}
