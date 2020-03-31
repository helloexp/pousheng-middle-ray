package com.pousheng.middle.consume.index.processor.core;

import lombok.Data;

import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-16 14:38<br/>
 */
@Data
public class IndexEvent {
    /**
     * 表名，例如 parana_orders
     */
    private String table;
    /**
     * 任务，例如 delta-dump
     */
    private String taskName;
    /**
     * 数据列，例如 [id, updated_at]
     */
    private List<String> cols;
    /**
     * 数据，例如 [[1, '2019-01-01 00:00:00'], [2, '2019-01-01 00:00:00']]
     */
    private List<List<String>> data;
}
