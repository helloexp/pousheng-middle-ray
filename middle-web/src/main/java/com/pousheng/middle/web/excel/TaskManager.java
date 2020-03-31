package com.pousheng.middle.web.excel;

import java.util.Set;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 14:29<br/>
 */
public interface TaskManager {
    void saveTask(String key, TaskMetaDTO meta);

    void deleteTask(String key) ;

    TaskMetaDTO getTask(String key);

    Set<String> getTasks(String taskType);
}
