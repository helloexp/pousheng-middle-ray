package com.pousheng.middle.web.order.job;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/10/12下午1:56
 */
@Slf4j
@Component
public class JdRedisHandler {

    private final static String JD_NOT_PAID_KEY = "PS:JD:NOT_PAID";

    @Autowired
    private JedisTemplate jedisTemplate;

    public void saveOrderId(ShopOrder shopOrder) {
        if(Objects.equals(MiddleChannel.JD.getValue(), shopOrder.getOutFrom())) {
            // 京东预售单
            Map<String, String> extraMap = shopOrder.getExtra();
            String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
            String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
            if (!StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
                if (!StringUtils.isEmpty(stepOrderStatus)
                        && (Objects.equals(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue(), Integer.valueOf(stepOrderStatus))
                        || Objects.equals(OpenClientStepOrderStatus.NOT_PAID.getValue(), Integer.valueOf(stepOrderStatus)))) {
                    // redis中已经存在则跳过
                    if(existValue(shopOrder.getOutId())) {
                        return;
                    }
                    // 存入redis
                    if(Objects.equals(MiddleChannel.JD.getValue(), shopOrder.getOutFrom())) {
                        jedisTemplate.execute(jedis -> {
                            jedis.lpush(JD_NOT_PAID_KEY, shopOrder.getOutId());
                        });
                    }
                }
            }
        }
    }


    /**
     * redis key is exist
     * @return
     */
    public Boolean existKey() {
        return jedisTemplate.execute(jedis -> {
            return jedis.exists(JD_NOT_PAID_KEY);
        });
    }

    /**
     * get key's value
     * @return
     */
    public List<String> getRedisValue() {
        if(existKey()) {
            List<String> result = jedisTemplate.execute(jedis -> {
                return jedis.lrange(JD_NOT_PAID_KEY, 0, -1);
            });
            return result;
        }
        return Collections.emptyList();
    }


    /**
     * value exist
     * @param outerOrderId
     * @return
     */
    public Boolean existValue(String outerOrderId) {
        List<String> result = getRedisValue();
        if(result.contains(outerOrderId)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * del redis
     */
    public void consume() {
        jedisTemplate.execute(jedis -> {
            jedis.rpop(JD_NOT_PAID_KEY);
        });
    }

}
