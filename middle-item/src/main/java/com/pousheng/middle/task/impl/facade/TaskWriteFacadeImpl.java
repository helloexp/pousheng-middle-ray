package com.pousheng.middle.task.impl.facade;

import com.google.common.base.Throwables;
import com.pousheng.middle.task.api.CreateTaskRequest;
import com.pousheng.middle.task.api.UpdateTaskRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.impl.converter.TaskLogicConverter;
import com.pousheng.middle.task.impl.service.TaskDomainReadService;
import com.pousheng.middle.task.impl.service.TaskDomainWriteService;
import com.pousheng.middle.task.model.Task;
import com.pousheng.middle.task.service.TaskWriteFacade;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-12 11:30<br/>
 */
@Slf4j
@Service
@RpcProvider
public class TaskWriteFacadeImpl implements TaskWriteFacade {
    private final TaskLogicConverter taskLogicConverter;
    private final TaskDomainReadService taskDomainReadService;
    private final TaskDomainWriteService taskDomainWriteService;

    public TaskWriteFacadeImpl(TaskLogicConverter taskLogicConverter, TaskDomainReadService taskDomainReadService, TaskDomainWriteService taskDomainWriteService) {
        this.taskLogicConverter = taskLogicConverter;
        this.taskDomainReadService = taskDomainReadService;
        this.taskDomainWriteService = taskDomainWriteService;
    }

    @Override
    public Response<Long> createTask(CreateTaskRequest request) {
        try {
            Task task = new Task();
            task.setStatus(request.getStatus());
            task.setType(request.getType());
            task.setContextJson(JsonMapper.nonEmptyMapper().toJson(request.getContent()));
            Long id = taskDomainWriteService.create(task);
            return Response.ok(id);
        } catch (Exception e) {
            log.error("fail to create task {}, cause:{}", request, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Boolean> updateTask(UpdateTaskRequest request) {
        try {
            Task found = taskDomainReadService.findOneById(request.getTaskId());
            if (found == null) {
                return Response.fail("task.not.found");
            }

            TaskDTO foundDto = taskLogicConverter.domain2dto(found);
            Task update = new Task();
            update.setId(request.getTaskId());
            update.setStatus(request.getStatus());
            update.setType(request.getType());

            if (!CollectionUtils.isEmpty(request.getContent())) {
                Map<String, Object> content = foundDto.getContent();
                if (CollectionUtils.isEmpty(content)) {
                    content = request.getContent();
                } else {
                    content.putAll(request.getContent());
                }
                update.setContextJson(JsonMapper.nonEmptyMapper().toJson(content));
            }

            taskDomainWriteService.update(update);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("fail , cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }
}
