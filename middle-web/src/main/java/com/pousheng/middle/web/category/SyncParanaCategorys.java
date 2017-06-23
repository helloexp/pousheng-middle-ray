package com.pousheng.middle.web.category;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.web.events.item.BatchSyncParanaCategoryEvent;
import com.pousheng.middle.web.task.SyncParanaTaskRedisHandler;
import com.pousheng.middle.web.task.SyncTask;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.open.client.parana.dto.OpenClientBackCategory;
import io.terminus.open.client.parana.item.SyncParanaCategoryService;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.BackCategoryReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by songrenfei on 2017/6/7
 */
@RestController
@Slf4j
@RequestMapping("/api/category")
public class SyncParanaCategorys {


    @RpcConsumer
    private BackCategoryReadService backCategoryReadService;
    @Autowired
    private SyncParanaCategoryService syncParanaCategoryService;
    @Autowired
    private SyncParanaTaskRedisHandler syncParanaTaskRedisHandler;
    @Autowired
    private EventBus eventBus;

    @RequestMapping(value = "/{id}/sync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> synCategory(@PathVariable(name = "id") Long categoryId){

        Response<BackCategory> categoryRes = backCategoryReadService.findById(categoryId);
        if(!categoryRes.isSuccess()){
            log.error("find category by id:{} fail,error:{}",categoryId,categoryRes.getError());
            throw new JsonResponseException(categoryRes.getError());
        }
        OpenClientBackCategory openClientBackCategory = new OpenClientBackCategory();
        BackCategory backCategory = categoryRes.getResult();
        BeanMapper.copy(backCategory,openClientBackCategory);
        return syncParanaCategoryService.syncBackCategory(openClientBackCategory);
    }


    /**
     * 全量同步类目
     * @return 任务ID
     */
    @RequestMapping(value = "/batch-sync", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public String batchSynCategory(){

        SyncTask task = new SyncTask();
        task.setStatus(1);
        String taskId = syncParanaTaskRedisHandler.saveTask(task);
        BatchSyncParanaCategoryEvent event = new BatchSyncParanaCategoryEvent();
        event.setTaskId(taskId);
        eventBus.post(event);
        return taskId;

    }
}
