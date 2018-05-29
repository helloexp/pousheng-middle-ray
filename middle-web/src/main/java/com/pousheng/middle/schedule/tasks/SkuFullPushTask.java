package com.pousheng.middle.schedule.tasks;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Description: 库存全量同步任务
 * Author: xiao
 * Date: 2018/05/29
 */
@Slf4j
@Component
public class SkuFullPushTask {
    private final JobLauncher jobLauncher;
    private final Job skuStockPartitionerJob;
    private final SqlSessionTemplate sqlSessionTemplate;


    /**
     * 初始化
     */
    public SkuFullPushTask(JobLauncher jobLauncher,
                           @Qualifier("skuStockPushJob")
                                   Job skuStockPartitionerJob,
                           SqlSessionTemplate sqlSessionTemplate) {
        this.jobLauncher = jobLauncher;
        this.skuStockPartitionerJob = skuStockPartitionerJob;
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /**
     * 触发设置
     */
    @Scheduled(cron = "0/10 * * * * *")
    public void trigger() {
        if (log.isDebugEnabled()) {
            log.debug("Run SkuStockPartitionerTask...");
        }


        int cnt = 0;

        while (true) {
            cnt ++;
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
            try {
                //Start Job
                JobExecution result = jobLauncher.run(skuStockPartitionerJob, parameters);
                log.info("SkuStockPartitionerJob result -------> {}", result);
            } catch (Exception e) {
                e.printStackTrace();
            }


            sqlSessionTemplate.update("SkuStockPartitioner.done", id);
            break;
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
