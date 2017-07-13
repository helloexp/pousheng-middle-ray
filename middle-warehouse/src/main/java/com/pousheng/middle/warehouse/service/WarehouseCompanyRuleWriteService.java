package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库写服务
 * Date: 2017-06-21
 */

public interface WarehouseCompanyRuleWriteService {

    /**
     * 创建WarehouseShopReturn
     * @param warehouseCompanyRule
     * @return 主键id
     */
    Response<Long> create(WarehouseCompanyRule warehouseCompanyRule);

    /**
     * 更新WarehouseShopReturn
     * @param warehouseCompanyRule
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseCompanyRule warehouseCompanyRule);

    /**
     * 根据主键id删除WarehouseShopReturn
     * @param warehouseShopReturnId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseShopReturnId);
}