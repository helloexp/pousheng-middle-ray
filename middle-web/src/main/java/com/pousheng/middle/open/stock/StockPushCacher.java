package com.pousheng.middle.open.stock;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/8/21
 */
@Component
@Slf4j
public class StockPushCacher {

    @Autowired
    private JedisTemplate jedisTemplate;
    private Float expirationTime;

    public static final String CACHE_PREFIX = "STOCK:";
    public static final String ORG_TYPE_WARE = "WARE:";
    public static final String ORG_TYPE_SHOP = "SHOP:";

    @Autowired
    public StockPushCacher(@Value("${stock.push.cache.duration.in.hours: 12}") float duration){
        this.expirationTime = 60*60*duration;
    }

    /**
     * @Description 将推送记录写入缓存
     * @Date        2018/7/4
     * @param       type
     * @param       org 如果type为WARE则为公司ID+仓库外码，如果是type为SHOP ，则为shop_id
     * @param       stockCode
     * @param       qty
     * @return
     */
    public void addToRedis(String type,String org,String stockCode,Integer qty){
        long startTime = System.currentTimeMillis();
        if(log.isDebugEnabled()){
            log.debug(" add pushed stock to cache, param: type = {},org = {},stockCode = {},qty = {}",type,org,stockCode,qty);
        }
        try {
            String key = CACHE_PREFIX + type + org + ":" + stockCode;
            String value = String.valueOf(qty);
            jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {

                @Override
                public void action(Jedis jedis) {
                    jedis.setex(key, expirationTime.intValue(), value);
                }
            });
        }catch (Exception e){
            log.error("setting cache for pushed stock failed,caused by {}",Throwables.getStackTraceAsString(e));
        }
        if(log.isDebugEnabled()){
            log.debug(" add pushed stock to cache, cost {}",System.currentTimeMillis()-startTime);
        }
    }


    /**
     * @Description 从缓存查询推送记录
     * @Date        2018/7/4
     * @param       type
     * @param       orgId
     * @param       stockCode
     * @return
     */
    public Integer getFromRedis(String type, String org, String stockCode){
        long startTime = System.currentTimeMillis();
        if(log.isDebugEnabled()){
            log.debug(" search pushed stock from cache, param:type = {},org = {},stockCode = {}",type,org,stockCode);
        }
        Integer result = null;
        try{
            String key = CACHE_PREFIX + type + org + ":" + stockCode;
            String qty = jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
                @Override
                public String action(Jedis jedis) {
                    return jedis.get(key);
                }
            });
            if(qty!=null){
                result = Integer.valueOf(qty);
            }
        }catch (Exception e){
            log.error("search pushed stock from cache failed,caused by {}",Throwables.getStackTraceAsString(e));
        }
        if(log.isDebugEnabled()){
            log.debug(" search pushed stock from cache result is {}, param :type = {},org = {},stockCode = {}",result,type,org,stockCode);
            log.debug(" search pushed stock from cache, cost {}",System.currentTimeMillis()-startTime);
        }
        return result;
    }

    public void del(String key){
        if(log.isDebugEnabled()){
            log.debug(" delete pushed stock from cache, key:{} ",key);
        }
        try {

            jedisTemplate.execute(jedis -> {
                jedis.del(key);
            });
        }catch (Exception e){
            log.error("delete cache for pushed stock failed,caused by {}",Throwables.getStackTraceAsString(e));
        }
    }


    /**
     * @Description 清楚所有库存推送记录缓存
     * @Date        2018/8/29
     * @param
     * @return
     */
    public void delAll(){
        Jedis jedis = jedisTemplate.getJedisPool().getResource();
        if (null == jedis) {
            log.error("error to get jedis resource");
            throw new ServiceException("jedis.is.null");
        }

        try {
            List<String> results = Lists.newArrayList();

            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams scanParams = new ScanParams();
            scanParams.count(2000);
            scanParams.match(CACHE_PREFIX);

            ScanResult<String> scanResult;
            do {
                scanResult = jedis.scan(cursor, scanParams);
                results.addAll(scanResult.getResult());
                cursor = scanResult.getStringCursor();
            } while(!"0".equals(cursor));

            for (String key : results) {
                this.del(key);
            }

        } catch (Exception e) {
            log.error("scan del data from redis for cache fail, key:{}, cause:{}", CACHE_PREFIX, Throwables.getStackTraceAsString(e));
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }

    }



}

