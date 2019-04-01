package com.pousheng.middle.web.task;

import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

/**
 * 同步任务 control
 * Created by songrenfei on 2017/4/14
 */
@Slf4j
@RestController
@RequestMapping("/api/sync/task")
public class SyncTasks implements Serializable{

    @Autowired
    private SyncParanaTaskRedisHandler importTaskRedisHandler;

    /**
     * 根据id获取导入任务
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public SyncTask pagingUser(@PathVariable("id") String taskId){
        if(log.isDebugEnabled()){
            log.debug("API-SYNC-TASK-START param: taskId [{}]",taskId);
        }
        SyncTask syncTask= importTaskRedisHandler.getTask(taskId);
        if(log.isDebugEnabled()){
            log.debug("API-SYNC-TASK-END param: taskId [{}] ,resp: [{}]",taskId,JsonMapper.nonEmptyMapper().toJson(syncTask));
        }
        return syncTask;
    }

}
