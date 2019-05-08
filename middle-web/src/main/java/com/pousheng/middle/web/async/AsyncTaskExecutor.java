package com.pousheng.middle.web.async;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.pousheng.middle.task.api.PagingTaskRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.service.TaskReadFacade;
import com.pousheng.middle.web.async.supplyRule.SkuSupplyRuleDisableTask;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步执行器
 * AUTHOR: zhangbin
 * ON: 2019/5/5
 */
@Slf4j
@Component
public class AsyncTaskExecutor {

    @Autowired
    private TaskReadFacade taskReadFacade;

    private Map<String, Class> taskMap = Maps.newHashMap();

    @PostConstruct
    public void register() {
        taskMap.put(TaskTypeEnum.SUPPLY_RULE_BATCH_DISABLE.name(), SkuSupplyRuleDisableTask.class);
    }

    public Response<Boolean> runTask(AsyncTask task) {
        ThreadPoolExecutor executor = task.getTaskExecutor();

        Response<Long> initResp;
        synchronized (this) {
            initResp = task.init();
        }

        if (!initResp.isSuccess()) {
            log.error("fail to run task, taskType:{}, cause:({})", task.getTaskType(), initResp.getError());
//            task.onError();
            return Response.fail(initResp.getError());
        }
        executor.submit(() -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            log.info("[AsyncTaskExecutor] submit start task:{}", task.getTaskId());
            try {
                task.preStart();
                task.start();
                task.onStop();
            } catch (Exception e) {
                log.error("[AsyncTaskExecutor] failed to process task {}, cause: {}", task, e);
                task.onError(e);
            }
            stopwatch.stop();
            log.info("[AsyncTaskExecutor] submit end task:{}, cost {} ms", task.getTaskId(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        });

        return Response.ok(Boolean.TRUE);
    }

    public TaskResponse lastStatus(TaskTypeEnum taskType) {
        try {
            AsyncTask asyncTask = (AsyncTask)taskMap.get(taskType.name()).newInstance();
            if (asyncTask == null) {
                return TaskResponse.empty();
            }
            PagingTaskRequest request = new PagingTaskRequest();
            request.setPageSize(1);
            request.setType(taskType.name());
            Response<Paging<TaskDTO>> paging = taskReadFacade.pagingTasks(request);
            if (paging.isSuccess() && !paging.getResult().getData().isEmpty()) {
                TaskDTO taskDTO = paging.getResult().getData().get(0);
                TaskResponse response = new TaskResponse();
                response.setInitDate(taskDTO.getCreatedAt());
                response.setType(taskDTO.getType());
                response.setContent(taskDTO.getContent());
                response.setStatus(taskDTO.getStatus());
                return response;
            }
        } catch (Exception e) {
            log.error("fail to get task:({}), cause:({})", taskType.name(), e);
        }
        return TaskResponse.empty();
    }

    public List<TaskDTO> findUnfinishedTasks(TaskTypeEnum type) {
        PagingTaskRequest request = new PagingTaskRequest();
        request.setPageNo(0);
        request.setPageSize(10);
        request.setStatus(TaskStatusEnum.EXECUTING.name());
        request.setType(type.name());
        Response<Paging<TaskDTO>> paging = taskReadFacade.pagingTasks(request);
        return paging.getResult().getData();
    }

    public AsyncTask getTask(TaskDTO task) {
        try {
            AsyncTask asyncTask = (AsyncTask)taskMap.get(task.getType()).newInstance();
            if (asyncTask == null) {
                return null;
            }
            return asyncTask.getTask(task);
        } catch (Exception e) {
            log.error("fail to get task:({}), cause:({})", task, e);
        }
        return null;
    }
}
