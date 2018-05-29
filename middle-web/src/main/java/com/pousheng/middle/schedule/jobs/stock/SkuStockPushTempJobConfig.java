package com.pousheng.middle.schedule.jobs.stock;

import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.schedule.jobs.BatchJobProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Description: 第三方推送任务配置
 * Author: xiao
 * Date: 2018/05/29
 */
@Configuration
public class SkuStockPushTempJobConfig {

    @Bean
    @StepScope
    public SkuStockPushTempPagingReader skuStockPushTempPagingReader(
            SqlSessionFactory sqlSessionFactory,
            BatchJobProperties properties) {
        SkuStockPushTempPagingReader itemReader = new SkuStockPushTempPagingReader();
        itemReader.setSqlSessionFactory(sqlSessionFactory);
        itemReader.setPageSize(properties.getStockFullDump().getPageSize());
        itemReader.setQueryId("not.use");
        return itemReader;
    }

    @Bean
    @StepScope
    public SkuStockPushTempProcessor skuStockPushTempProcessor(StockPusher stockPusher) {
        return new SkuStockPushTempProcessor(stockPusher);
    }

    @Bean
    @StepScope
    public SkuStockPushTempWriter skuStockPushTempWriter(SqlSessionTemplate sessionTemplate) {
        return new SkuStockPushTempWriter(sessionTemplate);
    }




    @Bean
    public SkuStockPushPartitionerPrepareTasklet skuStockPushPartitionerPrepareTasklet(
            SqlSessionTemplate sessionTemplate,
            BatchJobProperties batchJobProperties) {
        return new SkuStockPushPartitionerPrepareTasklet(sessionTemplate,
                batchJobProperties.getStockFullDump().getGridSize());
    }


    @Bean
    public Step partitionStep(StepBuilderFactory steps,
                              SkuStockPushPartitionerPrepareTasklet tasklet) {
        return steps.get("步骤1: 开始分片任务").tasklet(tasklet).build();
    }


    @Bean
    public TaskExecutor taskExecutor(BatchJobProperties properties) {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(properties.getStockFullDump().getThrottleLimit());
        return taskExecutor;
    }


    @Bean
    public Step processStep(StepBuilderFactory steps,
                            BatchJobProperties properties,
                            SkuStockPushTempPagingReader reader,
                            SkuStockPushTempProcessor processor,
                            SkuStockPushTempWriter writer,
                            TaskExecutor executor) {
        return steps.get("步骤2: 推送第三方平台")
                .<String, String>chunk(properties.getStockFullDump().getChunkSize())
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(executor)
                .throttleLimit(properties.getStockFullDump().getThrottleLimit()).build();
    }


    @Bean
    public Job skuStockPushJob(JobBuilderFactory jobs,
                              @Qualifier("processStep") Step step) {
        return jobs.get("任务: 推送库存至第三方平台").start(step).build();
    }


    @Bean
    public Job skuStockPartitionerJob(JobBuilderFactory jobs,
                               @Qualifier("partitionStep") Step step) {
        return jobs.get("任务: 准备库存推送分片").start(step).build();
    }

}
