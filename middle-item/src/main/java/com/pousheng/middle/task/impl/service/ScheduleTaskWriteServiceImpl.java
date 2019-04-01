package com.pousheng.middle.task.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import com.pousheng.middle.task.impl.dao.ScheduleTaskDao;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 任务信息表写服务实现类
 * Date: 2018-05-11
 */
@Slf4j
@Service
@RpcProvider
public class ScheduleTaskWriteServiceImpl implements ScheduleTaskWriteService {

    @Autowired
    private ScheduleTaskDao scheduleTaskDao;

    @Override
    public Response<Long> create(ScheduleTask scheduleTask) {
        try {
            scheduleTaskDao.create(scheduleTask);
            return Response.ok(scheduleTask.getId());
        } catch (Exception e) {
            log.error("create scheduleTask failed, scheduleTask:{}, cause:{}", scheduleTask, Throwables.getStackTraceAsString(e));
            return Response.fail("schedule.task.create.fail");
        }
    }


    @Override
    public Response<Integer> creates(List<ScheduleTask> scheduleTasks) {
        try {
            return Response.ok(scheduleTaskDao.creates(scheduleTasks));
        } catch (Exception e) {
            log.error("create scheduleTask failed, scheduleTask:{}, cause:{}", scheduleTasks, Throwables.getStackTraceAsString(e));
            return Response.fail("schedule.task.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ScheduleTask scheduleTask) {
        try {
            return Response.ok(scheduleTaskDao.update(scheduleTask));
        } catch (Exception e) {
            log.error("update scheduleTask failed, scheduleTask:{}, cause:{}", scheduleTask, Throwables.getStackTraceAsString(e));
            return Response.fail("schedule.task.update.fail");
        }
    }


    @Override
    public Response<Boolean> updateStatus(ScheduleTask scheduleTask, Integer targetStatus) {
        try {
            return Response.ok(scheduleTaskDao.updateStatus(scheduleTask,targetStatus));
        } catch (Exception e) {
            log.error("update scheduleTask failed, scheduleTask:{}, cause:{}", scheduleTask, Throwables.getStackTraceAsString(e));
            return Response.fail("schedule.task.update.fail");
        }
    }


    @Override
    public Response<Boolean> deleteById(Long scheduleTaskId) {
        try {
            return Response.ok(scheduleTaskDao.delete(scheduleTaskId));
        } catch (Exception e) {
            log.error("delete scheduleTask failed, scheduleTaskId:{}, cause:{}", scheduleTaskId, Throwables.getStackTraceAsString(e));
            return Response.fail("schedule.task.delete.fail");
        }
    }
}
