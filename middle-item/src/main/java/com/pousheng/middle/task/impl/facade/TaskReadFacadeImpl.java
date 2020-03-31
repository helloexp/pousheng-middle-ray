package com.pousheng.middle.task.impl.facade;

import com.google.common.base.Throwables;
import com.pousheng.middle.task.api.PagingTaskRequest;
import com.pousheng.middle.task.api.QuerySingleTaskByIdRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.impl.converter.CommonConverter;
import com.pousheng.middle.task.impl.converter.TaskLogicConverter;
import com.pousheng.middle.task.impl.service.TaskDomainReadService;
import com.pousheng.middle.task.model.Task;
import com.pousheng.middle.task.service.TaskReadFacade;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 17:13<br/>
 */
@Slf4j
@Service
@RpcProvider
public class TaskReadFacadeImpl implements TaskReadFacade {
    private final TaskLogicConverter taskLogicConverter;
    private final TaskDomainReadService taskDomainReadService;

    public TaskReadFacadeImpl(TaskLogicConverter taskLogicConverter, TaskDomainReadService taskDomainReadService) {
        this.taskLogicConverter = taskLogicConverter;
        this.taskDomainReadService = taskDomainReadService;
    }

    @Override
    public Response<Paging<TaskDTO>> pagingTasks(PagingTaskRequest request) {
        try {
            Paging<Task> found = taskDomainReadService.pagingByTypeAndStatus(request.getType(), request.getStatus(), request.getExclude(), request.getPageNo(), request.getPageSize());
            Paging<TaskDTO> page = CommonConverter.batchConvert(found, taskLogicConverter::domain2dto);
            return Response.ok(page);
        } catch (Exception e) {
            log.error("fail to paging task by {}, cause:{}", request, Throwables.getStackTraceAsString(e));
            return Response.fail("task.paging.failed");
        }
    }

    @Override
    public Response<TaskDTO> querySingleTaskById(QuerySingleTaskByIdRequest request) {
        try {
            Task found = taskDomainReadService.findOneById(request.getTaskId());
            return Response.ok(taskLogicConverter.domain2dto(found));
        } catch (Exception e) {
            log.error("fail find task by request {}, cause:{}", request, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }
}
