package com.pousheng.middle.consume.index.processor.core;

import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-16 15:32<br/>
 */
@Data
public class IndexEventProcessorWrap {
    private String table;
    private List<String> tasks;
    private IndexEventProcessor processor;
    private boolean matchAllTask;

    public IndexEventProcessorWrap(String table, String[] tasks, IndexEventProcessor processor) {
        this.table = table;
        this.processor = processor;
        if (tasks.length == 1 && "*".equals(tasks[0])){
            matchAllTask = true;
            this.tasks = Collections.singletonList("*");
            return;
        }
        this.matchAllTask = false;
        this.tasks = Arrays.asList(tasks);
    }

    public boolean accept(IndexEvent event) {
        if (!matchTable(event.getTable())) {
            return false;
        }
        return matchTask(event.getTaskName());
    }

    private boolean matchTable(String table) {
        return this.table.equalsIgnoreCase(table);
    }

    private boolean matchTask(String task) {
        if (matchAllTask) {
            return true;
        }
        return tasks.contains(task);
    }
}
