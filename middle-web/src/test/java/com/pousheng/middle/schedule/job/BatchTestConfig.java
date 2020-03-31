package com.pousheng.middle.schedule.job;

import com.pousheng.middle.schedule.jobs.BatchJobProperties;
import io.terminus.boot.mybatis.autoconfigure.MybatisAutoConfiguration;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.DatabaseType;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Description: 测试基础配置
 * User: xiao
 * Date: 28/06/2017
 */
@SuppressWarnings("all")
@Configuration
@AutoConfigureAfter(MybatisAutoConfiguration.class)
@EnableConfigurationProperties({
        BatchJobProperties.class
})
public class BatchTestConfig {
    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new ResourcelessTransactionManager();
    }

    @Bean
    public JobRepository jobRepository(javax.sql.DataSource dataSource,
                                       PlatformTransactionManager platformTransactionManager) throws Exception {
        JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
        factoryBean.setDatabaseType(DatabaseType.H2.name());
        factoryBean.setDataSource(dataSource);
        factoryBean.setTransactionManager(platformTransactionManager);
        return factoryBean.getObject();
    }

}
