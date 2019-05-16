package com.pousheng.middle.consume.index.processor.impl;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-22 16:31<br/>
 */
public class CommonBuilder {
    protected String timeString(Date input) {
        if (input == null) {
            return null;
        }
        return new DateTime(input).toString();
    }
}
