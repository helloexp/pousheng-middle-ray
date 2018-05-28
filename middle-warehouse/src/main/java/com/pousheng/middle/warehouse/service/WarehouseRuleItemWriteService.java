package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.enums.WarehouseRuleItemPriorityType;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则项写服务
 * Date: 2017-06-07
 */

public interface WarehouseRuleItemWriteService {

    /**
     * 批量保存WarehouseRuleItems
     *
     * @param ruleId 规则id
     * @param priorityType 优先级类型
     * @param warehouseRuleItem 列表
     * @return 是否创建成功
     */
    Response<Boolean> batchCreate(Long ruleId, WarehouseRuleItemPriorityType priorityType, List<WarehouseRuleItem> warehouseRuleItem);
}