package com.pousheng.middle.common.utils.batchhandle;

import java.lang.annotation.*;

/**
 * 导出标题名称
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ExportTitle {


    String value();


}
