package com.pousheng.middle.schedule.jobs;

import io.terminus.boot.mybatis.autoconfigure.MybatisAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


/**
 * Description: Spring Batch 配置
 * User: xiao
 * Date: 19/06/2017
 */
@Slf4j
@Configuration
@EnableBatchProcessing
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class BatchConfig {

    @Bean
    public JobRepository jobRepository(DataSource dataSource,
                                       PlatformTransactionManager platformTransactionManager) throws Exception {
        return BatchFactory.createJobRepository(dataSource, platformTransactionManager);
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) {
        return BatchFactory.createSimpleLauncher(jobRepository);
    }

    @Bean
    public ListableJobLocator jobRegistry() {
        return new MapJobRegistry();
    }


    @Bean
    public SimpleJobOperator jobOperator(JobExplorer jobExplorer, JobLauncher jobLauncher,
                                         JobRegistry jobRegistry, JobRepository jobRepository)
            throws Exception {
        return BatchFactory.createSimpleJobOperator(jobExplorer, jobLauncher, jobRegistry, jobRepository);
    }

}
