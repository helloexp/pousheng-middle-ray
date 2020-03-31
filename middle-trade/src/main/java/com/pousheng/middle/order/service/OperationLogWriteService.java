package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.OperationLog;
import io.terminus.common.model.Response;

/**
 * Author: sunbo
 * Desc: 写服务
 * Date: 2017-07-31
 */

public interface OperationLogWriteService {

    /**
     * 创建OperationLog
     * @param operationLog
     * @return 主键id
     */
    Response<Long> create(OperationLog operationLog);

    /**
     * 更新OperationLog
     * @param operationLog
     * @return 是否成功
     */
    Response<Boolean> update(OperationLog operationLog);

    /**
     * 根据主键id删除OperationLog
     * @param operationLogId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long operationLogId);
}