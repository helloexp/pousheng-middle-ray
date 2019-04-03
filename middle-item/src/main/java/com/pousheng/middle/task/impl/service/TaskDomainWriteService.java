package com.pousheng.middle.task.impl.service;

import com.pousheng.middle.task.impl.dao.TaskDao;
import com.pousheng.middle.task.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 15:36<br/>
 */
@Slf4j
@Component
public class TaskDomainWriteService {
    private final TaskDao taskDao;

    public TaskDomainWriteService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public Long create(Task task) {
        taskDao.create(task);
        return task.getId();
    }

    public boolean update(Task task) {
        return taskDao.update(task);
    }
}
