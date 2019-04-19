package com.pousheng.middle.task.service;

import com.pousheng.middle.task.api.PagingTaskRequest;
import com.pousheng.middle.task.api.QuerySingleTaskByIdRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;


/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 16:49<br/>
 */
public interface TaskReadFacade {
    Response<Paging<TaskDTO>> pagingTasks(PagingTaskRequest request);

    Response<TaskDTO> querySingleTaskById(QuerySingleTaskByIdRequest request);
}
