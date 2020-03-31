package com.pousheng.middle.consume.index.processor.impl;

import com.google.common.primitives.Longs;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-22 16:31<br/>
 */
public class CommonBuilder {
    /**
     * 支持 ES 默认识别时间格式
     */
    protected String timeString(Date input) {
        if (input == null) {
            return null;
        }
        return new DateTime(input).toString();
    }

    /**
     * 返回安全的数据字符串，null 对应空字符串
     */
    protected String numString(Number input) {
        return input == null ? "" : input.toString();
    }

    /**
     * 字符串到 Long，如果为空字符串对应 null
     */
    protected Long parseLong(String input) {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        return Longs.tryParse(input);
    }
}
