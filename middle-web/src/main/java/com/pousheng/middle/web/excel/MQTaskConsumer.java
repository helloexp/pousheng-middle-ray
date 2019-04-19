package com.pousheng.middle.web.excel;

import com.pousheng.middle.task.api.QuerySingleTaskByIdRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.service.TaskReadFacade;
import com.pousheng.middle.utils.ConditionalOnEnv;
import com.pousheng.middle.utils.MatchPolicy;
import com.pousheng.middle.web.excel.supplyRule.SupplyRuleImportTask;
import io.terminus.common.model.Response;
import io.terminus.common.rocketmq.annotation.ConsumeMode;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.annotation.MQSubscribe;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-17 16:52<br/>
 */
@Slf4j
@ConditionalOnProperty(value = "pousheng.supply-rule.import.enable", havingValue = "true")
@ConditionalOnEnv(name = "TERMINUS_HOST", havingValue = "pousheng.supply-rule.import.hosts", matchPolicy = MatchPolicy.MATCH_ANY, matchIfValueMissing = true)
@Component
@MQConsumer
public class MQTaskConsumer {
    public static final String TOPIC = "SimpleTask";
    public static final String GROUP = "DefaultGroup";
    public static final String TAG = "SupplyRuleImport";

    private TaskContainer taskContainer = new TaskContainer();

    private final TaskReadFacade taskReadFacade;

    public MQTaskConsumer(TaskReadFacade taskReadFacade) {
        this.taskReadFacade = taskReadFacade;
    }

    @PostConstruct
    public void setup() {
        taskContainer.start();
    }

    @MQSubscribe(topic = TOPIC, consumerGroup = GROUP, tag = TAG, consumeMode = ConsumeMode.CONCURRENTLY, consumeThreadMin = 1, consumeThreadMax = 2)
    public void onMessage(String message) throws InterruptedException {
        log.info("got message {}", message);
        UnifiedEvent event = JsonMapper.nonEmptyMapper().fromJson(message, UnifiedEvent.class);
        if (event.getEventTag().equals("start")) {
            onStartMessage(event.getPayload());
        } else {
            onStopMessage(event.getPayload());
        }
    }

    private void onStartMessage(String message) {
        TaskDTO taskDTO = JsonMapper.nonEmptyMapper().fromJson(message, TaskDTO.class);
        if (taskContainer.contains(taskDTO.getId())) {
            log.info("task already exist, skip {}", taskDTO);
            return;
        }
        SupplyRuleImportTask task = new SupplyRuleImportTask(taskDTO);
        TaskDTO exist = findTaskById(taskDTO.getId());
        if (exist == null || !Objects.equals(exist.getStatus(), TaskStatusEnum.INIT.name())) {
            return;
        }

        log.info("about to submit import task: {}", task);
        taskContainer.submit(task);
    }

    private void onStopMessage(String message) throws InterruptedException {
        Map<String, Object> stop = JsonMapper.nonEmptyMapper().fromJson(message, HashMap.class);
        Number id = (Number) stop.get("id");
        TaskDTO exist = findTaskById(id.longValue());
        if (exist == null ||
                !Objects.equals(exist.getStatus(), TaskStatusEnum.EXECUTING.name()) ||
                !Objects.equals(exist.getStatus(), TaskStatusEnum.INIT.name())) {
            log.info("task({}) is null or task status {} not match",
                    id,
                    Optional.ofNullable(exist).map(TaskDTO::getStatus).orElse(null));
            return;
        }

        log.info("about to kill import task: {}", stop);
        taskContainer.tryKill(id, ((Number) stop.get("timeout")).longValue(), TimeUnit.SECONDS);
    }

    private TaskDTO findTaskById(Long id) {
        Response<TaskDTO> r = taskReadFacade.querySingleTaskById(new QuerySingleTaskByIdRequest(id));
        if (!r.isSuccess()) {
            log.error("failed to find task by id: {}, cause: {}", id, r.getError());
            return null;
        }
        return r.getResult();
    }
}
