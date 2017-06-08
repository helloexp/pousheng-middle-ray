package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 仓库读服务
 * Date: 2017-06-07
 */

public interface WarehouseReadService {

    /**
     * 根据id查询仓库
     * @param Id 主键id
     * @return 仓库
     */
    Response<Warehouse> findById(Long Id);
}
