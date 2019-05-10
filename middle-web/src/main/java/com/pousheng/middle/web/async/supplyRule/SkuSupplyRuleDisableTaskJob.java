package com.pousheng.middle.web.async.supplyRule;

import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.web.async.AsyncTask;
import com.pousheng.middle.web.async.AsyncTaskExecutor;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/7
 */
@Slf4j
@ConditionalOnProperty(value = "pousheng.supply-rule.disable.enable", havingValue = "true", matchIfMissing = true)
@Component
public class SkuSupplyRuleDisableTaskJob {

    @Autowired
    private AsyncTaskExecutor executor;
    @Autowired
    private HostLeader hostLeader;

    @Scheduled(cron = "${pousheng.supply-rule.disable.cron:0 */10 * * * ?}")
    public void tryStopTask() {
        if(!hostLeader.isLeader()){
            log.info("[supply-rule-disable-task-check-task] another node is running, skip");
            return;
        }
        log.info("[supply-rule-disable-task-check-task] task start");
        List<TaskDTO> tasks = executor.findUnfinishedTasks(TaskTypeEnum.SUPPLY_RULE_BATCH_DISABLE);
        for (TaskDTO task : tasks) {
            AsyncTask unFinishedTasks = executor.getTask(task);
            if (unFinishedTasks == null || !unFinishedTasks.needStop()) {
                continue;
            }
            unFinishedTasks.manualStop();
        }
        log.info("[supply-rule-disable-task-check-task] task end, task size{}", tasks.size());
    }
}
