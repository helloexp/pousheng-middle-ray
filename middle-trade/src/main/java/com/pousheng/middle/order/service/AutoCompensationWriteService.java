package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.AutoCompensation;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Created by penghui on 2018/1/15
 */
public interface AutoCompensationWriteService {
    /**
     * 该方法用户同步任务失败后，新增自动补偿任务
     *
     * @param autoCompensation,需要判断name是否已经存在
     * @return id, 返回任务id
     */
    Response<Long> create(AutoCompensation autoCompensation);


    /**
     * 该方法可以用于更新自动补偿任务的状态
     *
     * @param autoCompensation
     * @return
     */
    Response<Boolean> update(AutoCompensation autoCompensation);

    /**
     * 批量更新状态
     *
     * @param ids    id集合
     * @param status 状态
     * @return
     */
    Response<Boolean> updateStatus(List<Long> ids, Integer status);

    /**
     * 长时间处于处理中的任务重置状态为待处理
     * @return
     */
    Response<Boolean> resetStatus();

}
