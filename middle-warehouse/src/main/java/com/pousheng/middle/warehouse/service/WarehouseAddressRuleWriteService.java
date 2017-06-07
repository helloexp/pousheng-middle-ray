package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联写服务
 * Date: 2017-06-07
 */

public interface WarehouseAddressRuleWriteService {

    /**
     * 创建WarehouseAddressRule
     * @param warehouseAddressRule
     * @return 主键id
     */
    Response<Long> create(WarehouseAddressRule warehouseAddressRule);

    /**
     * 更新WarehouseAddressRule
     * @param warehouseAddressRule
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseAddressRule warehouseAddressRule);

    /**
     * 根据主键id删除WarehouseAddressRule
     * @param warehouseAddressRuleId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseAddressRuleId);
}