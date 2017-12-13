package com.pousheng.middle.web.events.item.listener;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.PsSkuTemplateWriteService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.web.events.item.BatchAsyncHandleMposFlagEvent;
import com.pousheng.middle.web.events.item.SkuTemplateUpdateEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 监听异步批量处理mpos打标事件
 * @author penghui
 * @since 2017/12/11
 */
@Slf4j
@Component
public class BatchAsyncHandleMposFlagListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @RpcConsumer
    private PsSkuTemplateWriteService psSkuTemplateWriteService;

    @Autowired
    private JedisTemplate jedisTemplate;

    // 批处理数量
    private static final Integer BATCH_SIZE = 100;

    //存活时间 一个月
    private static final int BATCH_RECORD_EXPIRE_TIME = 2592000;

    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    @Subscribe
    public void onBatchMakeMposFlag(BatchAsyncHandleMposFlagEvent batchMakeMposFlagEvent){
        int pageNo = 1;
        String taskId = this.taskId();
        Map<String,String> params = batchMakeMposFlagEvent.getParams();
        Long userId = UserUtil.getUserId();
        String operateType = batchMakeMposFlagEvent.getType();
        String key = toKey(userId,operateType,taskId);
        //1.开始的时候记录状态
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTING);
            }
        });
        boolean next = batchHandle(pageNo,BATCH_SIZE,params,operateType,taskId);
        while(next){
            pageNo++;
            next = batchHandle(pageNo,BATCH_SIZE, params,operateType,taskId);
        }
        //3.结束后判断是否有异常记录，无显示完成，有显示有异常，并显示异常记录。
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                //1.如果有异常记录，置2，无异常，置1
                if(jedis.exists("mpos:"+ userId+":abnormal:flag:"+taskId)){
                    jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTE_ERROR);
                }else{
                    jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTED);
                }
            }
        });
    }

    /**
     * 分批次查询货品并进行处理
     * @param pageNo
     * @param size
     * @param params
     * @param operateType
     * @return
     */
    private Boolean batchHandle(int pageNo,int size,Map<String,String> params,String operateType,String taskId){
        String templateName = "search.mustache";
        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response = skuTemplateSearchReadService.searchWithAggs(pageNo,BATCH_SIZE, templateName, params, SearchSkuTemplate.class);
        if(!response.isSuccess()){
            log.error("fail to batch handle mpos flag，param={},cause:{}",params,response.getError());
            return Boolean.FALSE;
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getEntities().getData();
        if(response.getResult().getEntities().getTotal().equals(0L) || CollectionUtils.isEmpty(searchSkuTemplates)){
            return Boolean.FALSE;
        }
        for (SearchSkuTemplate searchSkuTemplate:searchSkuTemplates) {
            operateMposFlag(searchSkuTemplate.getId(),operateType,taskId);
        }
        int current = searchSkuTemplates.size();
        return current == size;
    }

    /**
     * 打标／取消打标
     * @param id
     * @param operateType 0 打标 1 取消打标
     */
    private void operateMposFlag(Long id,String operateType,String taskId){
        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}",id,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        Map<String,String> extra = operationMopsFlag(exist, operateType);
        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
            //有异常记录异常日志
            jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
                @Override
                public void action(Jedis jedis) {
                    String key = "mpos:"+ UserUtil.getUserId()+":abnormal:flag:"+taskId;
                    if(!jedis.exists(key)){
                        jedis.expire(key,BATCH_RECORD_EXPIRE_TIME);
                    }
                    jedis.lpush(key,exist.getExtra().get("materialCode")+"~"+resp.getError());
                }
            });
            throw new JsonResponseException(500, resp.getError());
        }
        postUpdateSearchEvent(id);
    }

    /**
     * 打标／取消打标
     * @param exist
     * @param type
     * @return
     */
    private Map<String,String> operationMopsFlag(SkuTemplate exist,String type){
        Map<String,String> extra = exist.getExtra();
        if(org.springframework.util.CollectionUtils.isEmpty(extra)){
            extra = Maps.newHashMap();
        }
        extra.put(PsItemConstants.MPOS_FLAG,type);
        return extra;
    }

    /**
     * 更新搜索
     * @param skuTemplateId
     */
    private void postUpdateSearchEvent(Long skuTemplateId){
        SkuTemplateUpdateEvent updateEvent = new SkuTemplateUpdateEvent();
        updateEvent.setSkuTemplateId(skuTemplateId);
        eventBus.post(updateEvent);
    }

    /**
     * 生成key
     * @param userId
     * @param operatorType
     * @param taskId
     * @return
     */
    private String toKey(Long userId,String operatorType,String taskId){
        return "mpos:" + userId + ":flag:" + operatorType + "~" + taskId;
    }

    /**
     * 生成任务ID
     * @return
     */
    private String taskId(){
        return String.valueOf(DateTime.now().toDate().getTime());
    }

}
