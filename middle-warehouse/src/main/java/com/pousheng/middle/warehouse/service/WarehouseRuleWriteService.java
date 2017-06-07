package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseRule;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述写服务
 * Date: 2017-06-07
 */

public interface WarehouseRuleWriteService {

    /**
     * 创建WarehouseRule
     * @param warehouseRule
     * @return 主键id
     */
    Response<Long> create(WarehouseRule warehouseRule);

    /**
     * 更新WarehouseRule
     * @param warehouseRule
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseRule warehouseRule);

    /**
     * 根据主键id删除WarehouseRule
     * @param warehouseRuleId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseRuleId);
}