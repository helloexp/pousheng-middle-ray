/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package dao;

import io.terminus.boot.mybatis.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Description: 单元测试设置
 * User: xiao
 * Date: 03/05/2017
 */
@Configuration
@ComponentScan({
        "com.pousheng.middle.group.impl.dao",
        "com.pousheng.middle.task.impl.dao",
})
@Import({DataSourceAutoConfiguration.class, MybatisAutoConfiguration.class})
class DaoConfiguration {}
