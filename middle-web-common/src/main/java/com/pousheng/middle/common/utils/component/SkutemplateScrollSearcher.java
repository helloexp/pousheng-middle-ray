package com.pousheng.middle.common.utils.component;

import com.google.common.base.Strings;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.Arguments;
import io.terminus.search.api.model.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.Map;

/**
 * Created by songrenfei on 2018/1/30
 */
@Component
@Slf4j
public class SkutemplateScrollSearcher {


    @Autowired
    private JedisTemplate jedisTemplate;

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;



    public <T> Response<? extends Pagination<T>> searchWithScroll(String contextId, Integer pageNo, Integer pageSize,
                                                                  String templateName, Map<String, String> params,
                                                                  Class<T> clazz) {


        String scrollId = "";
        if(Strings.isNullOrEmpty(contextId)){
            log.error("scroll search fail,context id invalid");
            return Response.fail("context.id.invalid");
        }

        String vals = doGetScrollId(contextId);
        if (!Strings.isNullOrEmpty(vals)) {
            scrollId = vals;
        }

        Response<? extends Pagination<T>> response = skuTemplateSearchReadService.searchWithScroll(scrollId,pageNo,pageSize,templateName,params,clazz);
        if(!response.isSuccess()){
            log.error("search by scroll fail,error:{}",response.getError());
            return Response.fail(response.getError());
        }

        scrollId = response.getResult().getScrollId();

        if(Arguments.notNull(scrollId)){
         setetScrollId(contextId,scrollId);
        }
        return response;
    }




    private String doGetScrollId(String contextId) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(contextId);
            }
        });
    }


    // 120秒到期时间
    public String setetScrollId(String contextId,String scrollId) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.setex(contextId,120,scrollId);
            }
        });
    }
}
