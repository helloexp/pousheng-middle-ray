package com.pousheng.middle.task.impl.dao;

import com.pousheng.middle.task.model.Task;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * 简单异步任务查询服务
 *
 * @author <a href="mailto:d@terminus.io">张成栋</a>
 * @date 2019-04-09 15:33:31
 */
@Repository
public class TaskDao extends MyBatisDao<Task> {
}
