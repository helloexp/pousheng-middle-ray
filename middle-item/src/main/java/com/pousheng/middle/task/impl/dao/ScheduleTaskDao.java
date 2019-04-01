package com.pousheng.middle.task.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.task.model.ScheduleTask;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Author: songrenfei
 * Desc: 任务信息表Dao类
 * Date: 2018-05-11
 */
@Repository
public class ScheduleTaskDao extends MyBatisDao<ScheduleTask> {

    public Boolean updateStatus(ScheduleTask scheduleTask,Integer targetStatus) {
        return this.sqlSession.update(this.sqlId("updateStatus"),ImmutableMap.of("id",scheduleTask.getId(),"status",scheduleTask.getStatus(),"targetStatus",targetStatus)) == 1;
    }


    public ScheduleTask findFirstByTypeAndStatus(Integer type,Integer status) {
        return getSqlSession().selectOne(sqlId("findFirstByTypeAndStatus"),  ImmutableMap.of("type", type, "status", status));
    }




}
