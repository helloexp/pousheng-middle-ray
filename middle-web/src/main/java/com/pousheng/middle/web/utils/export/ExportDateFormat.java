package com.pousheng.middle.web.utils.export;

import java.lang.annotation.*;

/**
 * 日期类型字段导出格式
 * Created by sunbo@terminus.io on 2017/7/21.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ExportDateFormat {

    String value();

}
