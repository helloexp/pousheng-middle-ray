package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联读服务
 * Date: 2017-06-07
 */

public interface WarehouseAddressRuleReadService {

    /**
     * 根据id查询地址和仓库规则的关联
     * @param Id 主键id
     * @return 地址和仓库规则的关联
     */
    Response<WarehouseAddressRule> findById(Long Id);
}
