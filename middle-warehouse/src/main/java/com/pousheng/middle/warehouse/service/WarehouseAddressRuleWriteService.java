package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.ThinAddress;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联写服务
 * Date: 2017-06-07
 */

public interface WarehouseAddressRuleWriteService {


    /**
     * 创建WarehouseAddresses
     * @param thinAddresses 仓库地址规则 列表
     * @return 对应的规则id
     */
    Response<Long> batchCreate(List<ThinAddress> thinAddresses);


    /**
     * 更新规则对应的warehouseAddresses
     * @param thinAddresses 仓库地址规则 列表
     * @return 对应的规则id
     */
    Response<Long> batchUpdate(Long ruleId, List<ThinAddress> thinAddresses);


    /**
     * 根据主键id删除WarehouseAddressRule
     * @param ruleId 规则id
     * @return 是否成功
     */
    Response<Boolean> deleteByRuleId(Long ruleId);
}