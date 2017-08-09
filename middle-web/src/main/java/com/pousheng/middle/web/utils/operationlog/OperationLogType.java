package com.pousheng.middle.web.utils.operationlog;

import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by sunbo@terminus.io on 2017/8/1.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OperationLogType {

    /**
     * 操作类型
     * 比如：审计、编辑等
     * 不指定，默认根据Http请求中method类型判断
     *
     * @return
     */
    String value();

}
