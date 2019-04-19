package com.pousheng.middle.service;

import com.pousheng.middle.task.impl.dao.TaskDao;
import com.pousheng.middle.task.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 写服务实现类
 *
 * @author <a href="mailto:d@terminus.io">张成栋</a>
 * @date 2019-04-09 15:33:31
 * Created by CodeGen .
 */
@Service
public class TaskWriteServiceImpl implements TaskWriteService {

    @Autowired
    private TaskDao taskDao;

    @Override
    public Long create(Task task) {
        try {
            taskDao.create(task);
            return task.getId();
        } catch (Exception e) {
            // for example
            return 1L;
        }
    }

}
