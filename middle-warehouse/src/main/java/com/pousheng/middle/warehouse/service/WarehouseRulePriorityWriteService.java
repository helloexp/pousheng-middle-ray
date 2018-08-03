package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import io.terminus.common.model.Response;

/**
 * @author: zhaoxiaowei
 * Desc: 写服务
 * Date: 2018-09-04
 */

public interface WarehouseRulePriorityWriteService {

    /**
     * 创建WarehouseRulePriority
     * @param warehouseRulePriority
     * @return 主键id
     */
    Response<Long> create(WarehouseRulePriority warehouseRulePriority);

    /**
     * 更新WarehouseRulePriority
     * @param warehouseRulePriority
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseRulePriority warehouseRulePriority);

    /**
     * 根据主键id删除WarehouseRulePriority
     * @param warehouseRulePriorityId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseRulePriorityId);
}
