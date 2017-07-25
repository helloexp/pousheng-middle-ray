package com.pousheng.middle.web.export;

import java.lang.annotation.*;

/**
 * 导出字段顺序
 * Created by sunbo@terminus.io on 2017/7/21.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ExportOrder {

    /**
     * 倒序。越大越在前
     *
     * @return
     */
    int value();
}
