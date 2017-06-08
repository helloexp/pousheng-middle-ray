package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联写服务
 * Date: 2017-06-07
 */

public interface WarehouseAddressRuleWriteService {

    /**
     * 创建WarehouseAddressRule
     * @param warehouseAddressRule 仓库地址规则
     * @return 主键id
     */
//    Response<Long> create(WarehouseAddressRule warehouseAddressRule);


    /**
     * 创建WarehouseAddresses
     * @param warehouseAddresses 仓库地址规则 列表
     * @return 对应的规则id
     */
    Response<Long> batchCreate(List<WarehouseAddress> warehouseAddresses);


    /**
     * 更新规则对应的warehouseAddresses
     * @param warehouseAddresses 仓库地址规则 列表
     * @return 对应的规则id
     */
    Response<Long> batchUpdate(Long ruleId, List<WarehouseAddress> warehouseAddresses);

    /**
     * 更新WarehouseAddressRule
     * @param warehouseAddressRule 仓库地址规则
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseAddressRule warehouseAddressRule);

    /**
     * 根据主键id删除WarehouseAddressRule
     * @param warehouseAddressRuleId 仓库地址规则id
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseAddressRuleId);
}