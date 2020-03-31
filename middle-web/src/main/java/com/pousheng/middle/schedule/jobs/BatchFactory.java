package com.pousheng.middle.schedule.jobs;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.DatabaseType;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


/**
 * Description: BatchFactory 工厂类
 * User: xiao
 * Date: 11/12/2017
 */
public class BatchFactory {

    /**
     * 创建一个标准的 Batch Repository，使用Mysql作为数据库
     */
    public static JobRepository createJobRepository(DataSource dataSource,
                                                    PlatformTransactionManager platformTransactionManager)
            throws Exception {
        JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
        factoryBean.setDatabaseType(DatabaseType.MYSQL.name());
        factoryBean.setDataSource(dataSource);
        factoryBean.setTransactionManager(platformTransactionManager);
        return factoryBean.getObject();
    }

    /**
     * 创建一个简单的 Job Launcher
     */
    public static JobLauncher createSimpleLauncher(JobRepository jobRepository) {
        SimpleJobLauncher launcher = new SimpleJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return launcher;
    }

    /**
     * 创建一个简单的 Job Operator
     */
    public static SimpleJobOperator createSimpleJobOperator(JobExplorer jobExplorer, JobLauncher jobLauncher,
                                                            JobRegistry jobRegistry, JobRepository jobRepository) {
        SimpleJobOperator factory = new SimpleJobOperator();
        factory.setJobExplorer(jobExplorer);
        factory.setJobLauncher(jobLauncher);
        factory.setJobRepository(jobRepository);
        factory.setJobRegistry(jobRegistry);
        return factory;
    }

}
