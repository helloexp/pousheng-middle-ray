package com.pousheng.middle.schedule.tasks;

import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Description: add something here
 * Author: xiao
 * Date: 2018/05/29
 */
@ConditionalOnProperty(name = "stock.job.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@Component
public class SkuStockPartitionerTask {
    private final JobLauncher jobLauncher;
    private final Job skuStockPartitionerJob;
    private final HostLeader hostLeader;

    /**
     * 初始化
     */
    public SkuStockPartitionerTask(JobLauncher jobLauncher,
                                   @Qualifier("skuStockPartitionerJob")
                                           Job skuStockPartitionerJob,
                                   HostLeader hostLeader) {
        this.jobLauncher = jobLauncher;
        this.skuStockPartitionerJob = skuStockPartitionerJob;
        this.hostLeader = hostLeader;
    }

    /**
     * 触发设置
     */
    @Scheduled(cron = "0 40 3 * * *")
    public void trigger() {
        if (!hostLeader.isLeader()) {
            log.info("Not leader, skipped");
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Run SkuStockPartitionerTask...");
        }

        JobParameters parameters = new JobParametersBuilder()
                .addDate("timestamp", DateTime.now().toDate())
                .toJobParameters();
        try {
            //Start Job
            JobExecution result = jobLauncher.run(skuStockPartitionerJob, parameters);
            log.info("SkuStockPartitionerJob result -------> {}", result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
