package com.pousheng.middle.web.excel.supplyRule;

import com.pousheng.middle.web.excel.TaskManager;
import com.pousheng.middle.web.excel.TaskMetaDTO;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.Objects;
import java.util.Set;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-08 14:26<br/>
 */
@Slf4j
// @Component
public class TaskTimeOutJob {
    private final TaskManager taskManager;
    private final HostLeader hostLeader;
    private String taskType = "supply-rule-import";

    public TaskTimeOutJob(TaskManager taskManager, HostLeader hostLeader) {
        this.taskManager = taskManager;
        this.hostLeader = hostLeader;
    }

    // @Scheduled(fixedRate = 300_000)
    public void scavenge() {
        log.info("[TASK_TIME_OUT_JOB] start scavenge task time out.");
        if (!hostLeader.isLeader()) {
            log.info("[TASK_TIME_OUT_JOB] i'm not host leader, i quit.");
            return;
        }

        Set<String> keys = taskManager.getTasks(taskType);
        if (keys.isEmpty()) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.info("[TASK_TIME_OUT_JOB] processing keys: {}", keys);
        }
        for (String key : keys) {
            TaskMetaDTO meta = taskManager.getTask(key);
            if (meta == null) {
                log.info("[TASK_TIME_OUT_JOB] task {} not found, skip", key);
                continue;
            }
            log.info("[TASK_TIME_OUT_JOB] processing task: {}", meta);

            // 超时三次删除
            if (Objects.equals(meta.getTimeout(), 3)) {
                log.info("[TASK_TIME_OUT_JOB] task {} timeout detected 3 times, delete it.", meta);
                taskManager.deleteTask(key);
                continue;
            }

            // 超时次数加1
            if (Objects.equals(meta.getManualStop(), 0)) {
                boolean timeout = DateTime.now().minusMinutes(5).isAfter(new DateTime(meta.getUpdatedAt()));
                if (timeout) {
                    int count = meta.getTimeout() + 1;
                    log.info("[TASK_TIME_OUT_JOB] task {} timeout detected {} times.", meta, count);
                    meta.setTimeout(count);
                    taskManager.saveTask(key, meta);
                }
            }
        }
    }
}
