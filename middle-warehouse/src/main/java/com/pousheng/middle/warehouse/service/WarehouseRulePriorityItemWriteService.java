package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import io.terminus.common.model.Response;

/**
 * @author: zhaoxiaowei
 * Desc: 写服务
 * Date: 2018-09-04
 */

public interface WarehouseRulePriorityItemWriteService {

    /**
     * 创建WarehouseRulePriorityItem
     * @param warehouseRulePriorityItem
     * @return 主键id
     */
    Response<Long> create(WarehouseRulePriorityItem warehouseRulePriorityItem);

    /**
     * 更新WarehouseRulePriorityItem
     * @param warehouseRulePriorityItem
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseRulePriorityItem warehouseRulePriorityItem);

    /**
     * 根据主键id删除WarehouseRulePriorityItem
     * @param warehouseRulePriorityItemId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseRulePriorityItemId);
}
