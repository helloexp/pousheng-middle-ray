package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库写服务
 * Date: 2017-06-21
 */

public interface WarehouseShopReturnWriteService {

    /**
     * 创建WarehouseShopReturn
     * @param warehouseShopReturn
     * @return 主键id
     */
    Response<Long> create(WarehouseShopReturn warehouseShopReturn);

    /**
     * 更新WarehouseShopReturn
     * @param warehouseShopReturn
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseShopReturn warehouseShopReturn);

    /**
     * 根据主键id删除WarehouseShopReturn
     * @param warehouseShopReturnId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseShopReturnId);
}