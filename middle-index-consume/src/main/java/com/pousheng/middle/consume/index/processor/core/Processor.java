/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.pousheng.middle.consume.index.processor.core;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-16 15:10<br/>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Processor {
    /**
     * 表名
     */
    @AliasFor("table")
    String value() default "";

    /**
     * 表名
     */
    @AliasFor("value")
    String table() default "";

    /**
     * 任务名
     */
    String[] task() default {"*"};
}
