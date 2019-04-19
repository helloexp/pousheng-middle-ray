package com.pousheng.middle.web.excel.supplyRule;

import com.pousheng.middle.task.api.PagingTaskRequest;
import com.pousheng.middle.task.api.UpdateTaskRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.service.TaskReadFacade;
import com.pousheng.middle.task.service.TaskWriteFacade;
import com.pousheng.middle.web.excel.AbstractSimpleTask;
import com.pousheng.middle.web.excel.TaskContainer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-08 14:27<br/>
 */
@ConditionalOnProperty(value = "pousheng.supply-rule.import.enable", havingValue = "true")
@Slf4j
@Component
public class TaskStarterJob {
    @Value("${task.maxWait:20}")
    private Integer maxWait;

    private final TaskContainer taskContainer;
    private final TaskReadFacade taskReadFacade;
    private final TaskWriteFacade taskWriteFacade;

    public TaskStarterJob(TaskContainer taskContainer, TaskReadFacade taskReadFacade, TaskWriteFacade taskWriteFacade) {
        this.taskContainer = taskContainer;
        this.taskReadFacade = taskReadFacade;
        this.taskWriteFacade = taskWriteFacade;
    }

    @Scheduled(fixedRate = 300_000)
    public void loadTasks() {
        if (taskContainer.taskCount() >= maxWait) {
            log.info("[TASK_STARTER_JOB] current task wait queue(size={}) is full.", taskContainer.taskCount());
            return;
        }

        AbstractSimpleTask[] tasks = taskContainer.getTasks();
        int existTaskCount = 0;
        List<Long> existTasks = null;
        if (tasks != null && tasks.length > 0) {
            existTasks = new ArrayList<>();
            existTaskCount = tasks.length;
            for (AbstractSimpleTask it : tasks) {
                existTasks.add(it.getTaskId());
            }
        }

        // load from db
        log.info("[TASK_STARTER_JOB] load task from db, current task queue(size={}, ids={}).", taskContainer.taskCount(), existTasks);
        PagingTaskRequest request = new PagingTaskRequest();
        request.setStatus(TaskStatusEnum.INIT.name());
        request.setType(TaskTypeEnum.SUPPLY_RULE_IMPORT.name());
        request.setExclude(existTasks);
        request.setPageNo(1);
        request.setPageSize(maxWait - existTaskCount);
        Response<Paging<TaskDTO>> response = taskReadFacade.pagingTasks(request);
        if (!response.isSuccess()) {
            log.error("failed to paging task by {}, cause: {}", request, response.getError());
            return;
        }

        List<TaskDTO> found = response.getResult().getData();
        if (CollectionUtils.isEmpty(found)) {
            return;
        }

        log.info("[TASK_STARTER_JOB] found {} new tasks from db.", found.size());
        found.forEach(it -> {
            try{
                taskContainer.submit(new SupplyRuleImportTask(it));
            } catch (IllegalArgumentException e){
                log.error("[TASK_STARTER_JOB] failed to submit task {}, cause: {}", it.getId(), e.getMessage());
                UpdateTaskRequest r = new UpdateTaskRequest();
                r.setTaskId(it.getId());
                r.setStatus(TaskStatusEnum.ERROR.name());
                taskWriteFacade.updateTask(r);
            }
        });
    }
}
