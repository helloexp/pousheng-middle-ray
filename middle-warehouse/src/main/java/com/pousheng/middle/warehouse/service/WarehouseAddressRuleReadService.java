package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.RuleDto;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联读服务
 * Date: 2017-06-07
 */

public interface WarehouseAddressRuleReadService {

    /**
     * 根据规则id查询地址和仓库规则的关联
     * @param ruleId 规则id
     * @return 规则概述
     */
    Response<RuleDto> findByRuleId(Long ruleId);

    /**
     *
     * @param ruleId
     * @return
     */
    Response<List<WarehouseAddress>> findAddressByRuleId(Long ruleId);
}
