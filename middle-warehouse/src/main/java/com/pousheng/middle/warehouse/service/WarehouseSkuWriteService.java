package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况写服务
 * Date: 2017-06-07
 */

public interface WarehouseSkuWriteService {

    /**
     * 创建WarehouseSku
     * @param warehouseSkuStock
     * @return 主键id
     */
    Response<Long> create(WarehouseSkuStock warehouseSkuStock);

    /**
     * 更新WarehouseSku
     * @param warehouseSkuStock
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseSkuStock warehouseSkuStock);

    /**
     * 根据主键id删除WarehouseSku
     * @param warehouseSkuId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseSkuId);
}