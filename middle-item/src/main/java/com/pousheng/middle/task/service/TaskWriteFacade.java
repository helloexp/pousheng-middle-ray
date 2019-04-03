package com.pousheng.middle.task.service;

import com.pousheng.middle.task.api.CreateTaskRequest;
import com.pousheng.middle.task.api.UpdateTaskRequest;
import io.terminus.common.model.Response;


/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 16:49<br/>
 */
public interface TaskWriteFacade {
    Response<Long> createTask(CreateTaskRequest request);

    Response<Boolean> updateTask(UpdateTaskRequest request);
}
