package com.pousheng.middle.task.service;

import com.pousheng.middle.task.dto.TaskSearchCriteria;
import com.pousheng.middle.task.model.ScheduleTask;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

/**
 * Author: songrenfei
 * Desc: 任务信息表读服务
 * Date: 2018-05-11
 */

public interface ScheduleTaskReadService {

    /**
     * 根据id查询任务信息表
     * @param Id 主键id
     * @return  查询结果
     */
    Response<ScheduleTask> findById(Long Id);


    /**
     * 根据类型和状态查找最早的记录
     * @param type
     * @param status
     * @return  查询结果
     */
    Response<ScheduleTask> findFirstByTypeAndStatus(Integer type, Integer status);


    /**
     * 根据条件查看任务列表
     * @param criteria 查询条件
     * @return 查询结果
     */
    Response<Paging<ScheduleTask>> paging(TaskSearchCriteria criteria);
}
