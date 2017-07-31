package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.OperationLog;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: sunbo
 * Desc: 读服务
 * Date: 2017-07-31
 */

public interface OperationLogReadService {

    /**
     * 根据id查询
     * @param Id 主键id
     * @return 
     */
    Response<OperationLog> findById(Long Id);
}
