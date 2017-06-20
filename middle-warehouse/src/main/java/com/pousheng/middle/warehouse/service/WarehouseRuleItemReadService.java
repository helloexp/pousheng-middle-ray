package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则项读服务
 * Date: 2017-06-07
 */

public interface WarehouseRuleItemReadService {

    /**
     * 根据id查询仓库优先级规则项
     * @param Id 主键id
     * @return 仓库优先级规则项
     */
    Response<WarehouseRuleItem> findById(Long Id);

    /**
     * 根据规则id查找关联的仓库
     *
     * @param ruleId 规则id
     * @return 规则关联的仓库
     */
    Response<List<WarehouseRuleItem>> findByRuleId(Long ruleId);
}
