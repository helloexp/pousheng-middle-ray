package com.pousheng.middle.task.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.task.dto.TaskSearchCriteria;
import com.pousheng.middle.task.impl.dao.ScheduleTaskDao;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: 任务信息表读服务实现类
 * Date: 2018-05-11
 */
@Slf4j
@Service
@RpcProvider
public class ScheduleTaskReadServiceImpl implements ScheduleTaskReadService {

    @Autowired
    private ScheduleTaskDao scheduleTaskDao;

    @Override
    public Response<ScheduleTask> findById(Long Id) {
        try {
            return Response.ok(scheduleTaskDao.findById(Id));
        } catch (Exception e) {
            log.error("find scheduleTask by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("schedule.task.find.fail");
        }
    }

    @Override
    public Response<ScheduleTask> findFirstByTypeAndStatus(Integer type, Integer status) {
        try {
            return Response.ok(scheduleTaskDao.findFirstByTypeAndStatus(type, status));
        } catch (Exception e) {
            log.error("find scheduleTask by type :{} status:{} failed,  cause:{}", type, status, Throwables.getStackTraceAsString(e));
            return Response.fail("schedule.task.find.fail");
        }
    }

    @Override
    public Response<Paging<ScheduleTask>> paging(TaskSearchCriteria criteria) {
        try {
            return Response.ok(scheduleTaskDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap()));
        } catch (Exception e) {
            log.error("find itemRule by criteria :{} failed,  cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("schedule.task.find.fail");
        }
    }


}
