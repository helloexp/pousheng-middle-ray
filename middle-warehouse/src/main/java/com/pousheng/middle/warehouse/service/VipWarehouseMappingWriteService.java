package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.VipWarehouseMapping;
import io.terminus.common.model.Response;

/**
 * Author: zhaoxiaowei
 * Desc: 写服务
 * Date: 2018-09-29
 */

public interface VipWarehouseMappingWriteService {

    /**
     * 创建VipWarehouseMapping
     *
     * @param vipWarehouseMapping
     * @return 主键id
     */
    Response<Long> create(VipWarehouseMapping vipWarehouseMapping);

    /**
     * 更新VipWarehouseMapping
     *
     * @param vipWarehouseMapping
     * @return 是否成功
     */
    Response<Boolean> update(VipWarehouseMapping vipWarehouseMapping);

    /**
     * 根据主键id删除VipWarehouseMapping
     *
     * @param vipWarehouseMappingId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long vipWarehouseMappingId);


    /**
     * 根据主键仓库删除VipWarehouseMapping
     *
     * @param warehouseId
     * @return 是否成功
     */
    Response<Boolean> deleteByWarehouseId(Long warehouseId);
}
