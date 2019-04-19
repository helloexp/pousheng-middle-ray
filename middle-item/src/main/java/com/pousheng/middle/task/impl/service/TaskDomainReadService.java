package com.pousheng.middle.task.impl.service;

import com.google.common.collect.Maps;
import com.pousheng.middle.task.impl.dao.TaskDao;
import com.pousheng.middle.task.model.Task;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 15:36<br/>
 */
@Slf4j
@Component
public class TaskDomainReadService {
    private final TaskDao taskDao;

    public TaskDomainReadService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public Paging<Task> pagingByTypeAndStatus(String type, String status, List<Long> exclude, Integer pageNo, Integer pageSize) {
        PageInfo page = PageInfo.of(pageNo, pageSize);
        Map<String, Object> conditions = Maps.newHashMap();
        conditions.putAll(page.toMap());
        conditions.put("status", status);
        conditions.put("type", type);
        conditions.put("exclude", exclude);

        return taskDao.paging(conditions);
    }

    public Task findOneById(Long taskId) {
        return taskDao.findById(taskId);
    }
}
