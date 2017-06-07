package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 仓库写服务
 * Date: 2017-06-07
 */

public interface WarehouseWriteService {

    /**
     * 创建Warehouse
     * @param warehouse
     * @return 主键id
     */
    Response<Long> create(Warehouse warehouse);

    /**
     * 更新Warehouse
     * @param warehouse
     * @return 是否成功
     */
    Response<Boolean> update(Warehouse warehouse);

    /**
     * 根据主键id删除Warehouse
     * @param warehouseId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseId);
}