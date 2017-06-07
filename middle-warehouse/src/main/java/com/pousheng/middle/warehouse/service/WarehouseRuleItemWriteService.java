package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则项写服务
 * Date: 2017-06-07
 */

public interface WarehouseRuleItemWriteService {

    /**
     * 创建WarehouseRuleItem
     * @param warehouseRuleItem
     * @return 主键id
     */
    Response<Long> create(WarehouseRuleItem warehouseRuleItem);

    /**
     * 更新WarehouseRuleItem
     * @param warehouseRuleItem
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseRuleItem warehouseRuleItem);

    /**
     * 根据主键id删除WarehouseRuleItem
     * @param warehouseRuleItemId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseRuleItemId);
}