package com.pousheng.middle.task.service;

import com.pousheng.middle.task.model.ScheduleTask;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 任务信息表写服务
 * Date: 2018-05-11
 */

public interface ScheduleTaskWriteService {

    /**
     * 创建ScheduleTask
     * @param scheduleTask
     * @return 主键id
     */
    Response<Long> create(ScheduleTask scheduleTask);


    /**
     * 创建ScheduleTask
     * @param scheduleTasks
     * @return 主键id
     */
    Response<Integer> creates(List<ScheduleTask> scheduleTasks);

    /**
     * 更新ScheduleTask
     * @param scheduleTask
     * @return 是否成功
     */
    Response<Boolean> update(ScheduleTask scheduleTask);


    /**
     * 更新ScheduleTask
     * @param scheduleTask
     * @return 是否成功
     */
    Response<Boolean> updateStatus(ScheduleTask scheduleTask,Integer targetStatus);

    /**
     * 根据主键id删除ScheduleTask
     * @param scheduleTaskId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long scheduleTaskId);
}
