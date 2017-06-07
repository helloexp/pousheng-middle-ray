package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseSku;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况写服务
 * Date: 2017-06-07
 */

public interface WarehouseSkuWriteService {

    /**
     * 创建WarehouseSku
     * @param warehouseSku
     * @return 主键id
     */
    Response<Long> create(WarehouseSku warehouseSku);

    /**
     * 更新WarehouseSku
     * @param warehouseSku
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseSku warehouseSku);

    /**
     * 根据主键id删除WarehouseSku
     * @param warehouseSkuId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseSkuId);
}