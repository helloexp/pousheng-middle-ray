package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.RuleDto;
import com.pousheng.middle.warehouse.dto.WarehouseAddressDto;
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
     * 根据仓库优先级规则id, 返回对应的仓库发货地址信息
     *
     * @param ruleId 规则id
     * @return 仓库发货地址信息
     */
    Response<List<WarehouseAddressDto>> findAddressByRuleId(Long ruleId);
}
