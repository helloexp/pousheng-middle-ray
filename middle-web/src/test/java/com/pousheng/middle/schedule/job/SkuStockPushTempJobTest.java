package com.pousheng.middle.schedule.job;

import com.pousheng.middle.schedule.jobs.stock.SkuStockPushTempJobConfig;
import io.terminus.boot.mybatis.autoconfigure.MybatisAutoConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * Description: 对库存全量推送脚本的单测
 * Author: xiao
 * Date: 2018/05/29
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Rollback
@ActiveProfiles("batch")
public class SkuStockPushTempJobTest {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTime DATE20160101 = DateTime.parse("2016-01-01", DateTimeFormat.forPattern(DATE_FORMAT));


    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;


    @Before
    public void init() {
        // 假定当前日期为 2016-6-1
        DateTimeUtils.setCurrentMillisFixed(DATE20160101.getMillis());
    }


    @Test
    public void testExecution() throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addDate("timestamp", DateTime.now().toDate())
                .toJobParameters();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(parameters);
        assertEquals(jobExecution.getStatus(), BatchStatus.COMPLETED);
    }



    @Import({
            DataSourceAutoConfiguration.class,
            MybatisAutoConfiguration.class,
            BatchTestConfig.class,
            SkuStockPushTempJobConfig.class
    })
    @Configuration
    @EnableBatchProcessing
    static class Config implements ApplicationListener<ContextRefreshedEvent> {
        private SqlSessionTemplate spy(SqlSessionTemplate sqlSessionTemplate) {
            SqlSessionTemplate spy = Mockito.spy(sqlSessionTemplate);


            return spy;
        }

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            ApplicationContext ctx = event.getApplicationContext();
            // inject a spy
        }

        @Bean
        @Primary
        public JobLauncherTestUtils jobLauncherTestUtils() {
            return new JobLauncherTestUtils() {
                @Override
                @Autowired
                public void setJob(@Qualifier("skuStockPushJob") Job job) {
                    super.setJob(job);
                }
            };
        }
    }

}
