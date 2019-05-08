package com.pousheng.middle.web.async;

import com.pousheng.middle.task.dto.TaskDTO;
import io.terminus.common.model.Response;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/5
 */
public interface AsyncTask {


    /**
     * 这里判断任务是否需要停止，如是否超时
     */
    Boolean needStop();

    /**
     * 任务id
     */
    Long getTaskId();

    /**
     * 任务类型
     * @see com.pousheng.middle.task.enums.TaskTypeEnum
     */
    String getTaskType();

    /**
     * 执行状态
     */
    TaskResponse getLastStatus(String taskType);

    ThreadPoolExecutor getTaskExecutor();

    Response<Long> init();

    void preStart();

    void start();

    void onStop();

    void onError(Exception e);

    void manualStop();

    AsyncTask getTask(TaskDTO task);
}
