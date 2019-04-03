package com.pousheng.middle.utils;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-18 10:17<br/>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnEnvCondition.class)
public @interface ConditionalOnEnv {
    String name() default "";

    String havingValue() default "";

    MatchPolicy matchPolicy() default MatchPolicy.MATCH_ANY;

    boolean matchIfMissing() default false;

    boolean matchIfEnvMissing() default false;

    boolean matchIfValueMissing() default false;
}
