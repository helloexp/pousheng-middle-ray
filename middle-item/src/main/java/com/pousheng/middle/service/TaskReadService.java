package com.pousheng.middle.service;

import com.pousheng.middle.task.model.Task;

/**
 * 读服务接口
 *
 * @author <a href="mailto:d@terminus.io">张成栋</a>
 * @date 2019-04-09 15:33:31
 * Created by CodeGen .
 */
public interface TaskReadService {

    /**
     * 查询
     *
     * @param id
     * @return task
     */
    Task findById(Long id);

}
