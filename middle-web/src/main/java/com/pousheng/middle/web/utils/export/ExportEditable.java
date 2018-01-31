package com.pousheng.middle.web.utils.export;

import java.lang.annotation.*;

/**
 * 导出字段是否可写
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.TYPE})
@Documented
public @interface ExportEditable {

    boolean value() default false;
}
