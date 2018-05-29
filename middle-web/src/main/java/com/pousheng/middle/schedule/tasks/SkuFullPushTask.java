package com.pousheng.middle.schedule.tasks;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.SimpleJobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.isNull;

/**
 * Description: 库存全量同步任务
 * Author: xiao
 * Date: 2018/05/29
 */

@ConditionalOnProperty(name = "stock.job.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@Component
public class SkuFullPushTask {

    private final JobLauncher jobLauncher;
    private final Job skuStockPartitionerJob;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final JobExplorer jobExplorer;


    /**
     * 初始化
     */
    public SkuFullPushTask(JobLauncher jobLauncher,
                           @Qualifier("skuStockPushJob")
                                   Job skuStockPartitionerJob,
                           SqlSessionTemplate sqlSessionTemplate, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.skuStockPartitionerJob = skuStockPartitionerJob;
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.jobExplorer = jobExplorer;
    }

    /**
     * 触发设置
     */
    @Scheduled(cron = "0 0/1 * * * *")
    public void trigger() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Run SkuStockPartitionerTask...");
            }

            Set<JobExecution> jobExecutions = jobExplorer
                    .findRunningJobExecutions(skuStockPartitionerJob.getName());
            if (jobExecutions.size() > 1) {
//                for (JobExecution jobExecution : jobExecutions) {
//                    if (new DateTime(jobExecution.getCreateTime()).plusMinutes(1).isBefore(DateTime.now())) {
//                        jobExecution.
//                    }
//                }

                log.info("SkuStockPartitionerJob {} running, skipped", skuStockPartitionerJob.getName());
                return;
            }


            int cnt = 0;
            while (true) {
                cnt++;
                if (cnt > 5) {
                    log.warn("Retry greater than 5, return");
                    return;
                }


                if (!checkIfNeedProcess()) {
                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Try to fetch task");
                }

                Map<String, Object> part = sqlSessionTemplate.selectOne("SkuStockPartitioner.fetch");

                // 尝试上锁
                if (isNull(part) || isNull(part.get("id")) || isNull(part.get("start")) || isNull(part.get("end"))) {
                    return;
                }

                log.info("We got an task {}, try to lock..", part);


                String ip;
                try {
                    ip = InetAddress.getLocalHost().getHostAddress();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                Long id = Long.parseLong(part.get("id").toString());
                int affect = sqlSessionTemplate.update("SkuStockPartitioner.lock",
                        ImmutableMap.of("id", id, "machine", ip));
                if (affect == 0) {
                    log.info("Locked failed");
                    continue;
                }
                log.info("Locked id {} successfully", id);

                Long start = Long.parseLong(part.get("start").toString());
                Long end = Long.parseLong(part.get("end").toString());

                JobParameters parameters = new JobParametersBuilder()
                        .addDate("timestamp", DateTime.now().toDate())
                        .addLong("start", start)
                        .addLong("end", end)
                        .toJobParameters();

                //Start Job
                JobExecution result = jobLauncher.run(skuStockPartitionerJob, parameters);
                log.info("SkuStockPartitionerJob result -------> {}", result);
                sqlSessionTemplate.update("SkuStockPartitioner.done", id);
                break;
            }


        } catch (Exception e) {
            log.error("Failed to push fully, cause {}", Throwables.getStackTraceAsString(e));
        }
    }

    private boolean checkIfNeedProcess() {
        Long count = sqlSessionTemplate.selectOne("SkuStockPartitioner.count",
                ImmutableMap.of("statusList", Lists.newArrayList("INIT")));
        if (log.isDebugEnabled()) {
            log.debug("There is {} task wait to run...", count);
        }
        return count > 0;
    }

}
