package com.pousheng.middle.web.task;

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
        return importTaskRedisHandler.getTask(taskId);
    }

}
