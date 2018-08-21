
package com.pousheng.middle.web.order.job;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by penghui on 2018/6/12
 */
@Slf4j
public abstract class AbstractAsyncJob {

    @Autowired
    private JedisTemplate jedisTemplate;

    @Autowired
    private HostLeader hostLeader;

    /**
     * 〈获取不通类型key〉
     *
     * @return: Author:xiehong
     * Date: 2018/6/12 上午9:45
     */
    protected abstract String getKeySuffix();

    /**
     * 获取任务id集合
     *
     * @return
     */
    protected abstract Map<String, List<Long>> getPushIds();

    /**
     * 任务状态置为待处理
     *
     * @param ids
     */
    protected abstract void waitHandle(List<Long> ids);

    /**
     * 消费任务
     *
     * @param ids
     */
    protected abstract void consume(List<Long> ids);

    /**
     * 获取线程池当前空闲线程数
     *
     * @return
     */
    protected abstract Integer getPopSize();


    /**
     * 单生产
     */
    public void producer() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is {}, skip", hostLeader.currentLeaderId());
            return;
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[START TO PRODUCER {} TASK]", getKeySuffix());
        Map<String, List<Long>> map = getPushIds();
        if (log.isDebugEnabled()) {
            log.debug("producer:{} get {} group task", getKeySuffix(), map.size());
        }
        map.forEach((key, ids) -> {
            if (!CollectionUtils.isEmpty(ids)) {
                waitHandle(ids);
                jedisTemplate.execute(jedis -> {
                    jedis.lpush(getQueueKey(), JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(ids));
                });
            }
        });
        stopwatch.stop();
        if (log.isDebugEnabled()) {
            log.debug("[END TO PRODUCER {} TASK],and cost {} seconds", getKeySuffix(), stopwatch.elapsed(TimeUnit.SECONDS));
        }
    }

    /**
     * 多消费
     */
    public void consumer() {
        new Thread(new ConsumerTask()).start();
    }

    /**
     * 获取redis队列key
     */
    private String getQueueKey() {
        return "PS:JOB:" + getKeySuffix();
    }

    /**
     * 获取动态参数，每组数量key
     *
     * @return
     */
    private String getGroupSizeKey() {
        return "PS:GROUP:" + getKeySuffix();
    }

    /**
     * 控制是否生产key
     *
     * @return
     */
    private String getProduceSwitchKey() {
        return "PS:PRODUCER:SWITCH:" + getKeySuffix();
    }

    /**
     * 控制是否消费key
     *
     * @return
     */
    private String getConsumeSwitchKey() {
        return "PS:CONSUME:SWITCH:" + getKeySuffix();
    }

    /**
     * 动态获取每组数量
     *
     * @return
     */
    public Integer size() {
        try {
            String size = jedisTemplate.execute(jedis -> {
                return jedis.get(getGroupSizeKey());
            });
            if (StringUtils.isEmpty(size)) {
                return null;
            }
            return Integer.valueOf(size);
        } catch (Exception e) {
            log.error("get group size failed,cause:{}", Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    /**
     * 判断是否允许生产
     *
     * @return
     */
    public Boolean canProduce() {
        String swit = jedisTemplate.execute(jedis -> {
            return jedis.get(getProduceSwitchKey());
        });
        return !Objects.equals(swit, "off");
    }

    /**
     * 判断是否允许消费
     *
     * @return
     */
    public Boolean canConsume() {
        String swit = jedisTemplate.execute(jedis -> {
            return jedis.get(getConsumeSwitchKey());
        });
        return !Objects.equals(swit, "off");
    }

    /**
     * 业务类型
     */
    public enum BizType {

        FIRST(1, "yyedi同步发货"),
        SECOND(2, "other");

        private Integer value;

        private String desc;

        BizType(Integer value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public Integer val() {
            return this.value;
        }
    }

    public class ConsumerTask implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            try {
                while (true) {
                    int groupSize = getPopSize();
                    if (log.isDebugEnabled()) {
                        log.debug("[CONSUMER CURRENT THREAD POOL REMAIN SIZE : {}]", groupSize);
                    }
                    if (groupSize == 0) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            log.error("thread sleep failed");
                            Thread.currentThread().interrupt();
                        }
                    }
                    for (int i = 0; i < groupSize; i++) {
                        String idStr = jedisTemplate.execute(jedis -> {
                            return jedis.rpop(getQueueKey());
                        });
                        if (StringUtils.isEmpty(idStr)) {
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException e) {
                                log.error("thread sleep failed");
                                Thread.currentThread().interrupt();
                            }
                            continue;
                        }
                        List<Long> ids = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(idStr, JsonMapper.nonEmptyMapper().createCollectionType(List.class, Long.class));
                        consume(ids);
                    }
                }
            } catch (Exception e) {
                log.warn("fail to consumer queue:{}", getQueueKey());
            }
        }
    }
}
