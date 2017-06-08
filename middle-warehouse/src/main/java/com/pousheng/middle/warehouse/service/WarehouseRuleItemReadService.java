package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import io.terminus.common.model.Response;

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
}
