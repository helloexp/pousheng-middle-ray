package com.pousheng.middle.web.events.item.listener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.events.item.BatchSyncParanaCategoryEvent;
import com.pousheng.middle.web.task.SyncErrorData;
import com.pousheng.middle.web.task.SyncParanaTaskRedisHandler;
import com.pousheng.middle.web.task.SyncTask;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.parana.dto.OpenClientBackCategory;
import io.terminus.open.client.parana.item.SyncParanaCategoryService;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.BackCategoryReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/7
 */
@Slf4j
@Component
public class BatchSyncParanaCategoryListener {


    @Autowired
    private EventBus eventBus;

    @Autowired
    private BackCategoryCacher backCategoryCacher;
    @RpcConsumer
    private BackCategoryReadService backCategoryReadService;
    @Autowired
    private SyncParanaCategoryService syncParanaCategoryService;
    @Autowired
    private SyncParanaTaskRedisHandler syncParanaTaskRedisHandler;

    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    @Subscribe
    public void onSyncCategory(BatchSyncParanaCategoryEvent event){

        log.info("dump sync category to parana start");
        String taskId = event.getTaskId();

        List<BackCategory> allCategories = Lists.newArrayList();
        findAllCategory(allCategories,0L);
        List<SyncErrorData> errorDatas = Lists.newArrayListWithCapacity(allCategories.size());
        for (BackCategory backCategory : allCategories){
            System.out.println(backCategory);
            Response<Boolean> syncRes = sync(backCategory);
            if(!syncRes.isSuccess()){
                log.error("sync back category(id:{}) to parana fail,error:{}",backCategory.getId(),syncRes.getError());
                SyncErrorData errorData = new SyncErrorData();
                errorData.setId(backCategory.getId());
                errorData.setName(backCategory.getName());
                errorData.setError(syncRes.getError());
                errorDatas.add(errorData);
            }

        }

        if (!Arguments.isNullOrEmpty(errorDatas)){
            log.error("sync category failed data = {}", errorDatas);
            //更新redis task状态
            SyncTask task = new SyncTask();
            task.setStatus(-1);
            task.setError(JsonMapper.nonDefaultMapper().toJson(errorDatas));
            syncParanaTaskRedisHandler.updateTask(taskId,task);

        }else {
            log.info("sync category to  parana success......");
            //更新redis task状态
            SyncTask task = new SyncTask();
            task.setStatus(2);
            syncParanaTaskRedisHandler.updateTask(taskId,task);

        }


        log.info("dump sync category to parana end");

    }

    private Response<Boolean> sync(BackCategory backCategory){
        OpenClientBackCategory openClientBackCategory = new OpenClientBackCategory();
        BeanMapper.copy(backCategory,openClientBackCategory);
        //return syncParanaCategoryService.syncBackCategory(openClientBackCategory);
        return Response.ok(Boolean.TRUE);
    }


    private List<BackCategory> findAllCategory(List<BackCategory> allCategories,Long pid){

        List<BackCategory> categories = findChildWithCacher(pid);

        for (BackCategory category : categories){

            allCategories.add(category);
            if(!category.getLevel().equals(4)){
                findAllCategory(allCategories,category.getId());
            }

        }

        return allCategories;

    }

    private List<BackCategory> findChildWithCacher(Long pid){
        return backCategoryCacher.findChildrenOf(pid);
    }

    private List<BackCategory> findChildWithoutCacher(Long pid){
        Response<List<BackCategory>> childRes = backCategoryReadService.findChildrenByPid(pid);
        if(!childRes.isSuccess()){
            log.error("find child category by pid:{} fail,error:{}",pid,childRes.getError());
            return Lists.newArrayList();
        }
        return childRes.getResult();
    }
}
