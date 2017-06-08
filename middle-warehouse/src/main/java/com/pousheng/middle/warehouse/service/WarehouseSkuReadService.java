package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseSku;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况读服务
 * Date: 2017-06-07
 */

public interface WarehouseSkuReadService {

    /**
     * 根据id查询sku在仓库的库存情况
     * @param Id 主键id
     * @return sku在仓库的库存情况
     */
    Response<WarehouseSku> findById(Long Id);
}
