package com.pousheng.middle.schedule.jobs;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Description: 计划任务配置
 * User: xiao
 * Date: 29/09/2017
 */
@Configuration
@ComponentScan({
        "com.pousheng.middle.schedule.jobs",
        "com.pousheng.middle.schedule.tasks",
        "com.pousheng.middle.schedule.rest"
})
public class TaskConfig {}
