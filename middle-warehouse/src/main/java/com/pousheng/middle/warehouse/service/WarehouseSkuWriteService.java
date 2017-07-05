package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.SelectedWarehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况写服务
 * Date: 2017-06-07
 */

public interface WarehouseSkuWriteService {

    /**
     * 创建WarehouseSku
     *
     * @param warehouseSkuStock 待创建的库存
     * @return 主键id
     */
    Response<Long> create(WarehouseSkuStock warehouseSkuStock);

    /**
     * 更新WarehouseSku
     *
     * @param warehouseSkuStock 待更新的库存
     * @return 是否成功
     */
    Response<Boolean> update(WarehouseSkuStock warehouseSkuStock);

    /**
     * 根据主键id删除WarehouseSku
     *
     * @param warehouseSkuId 仓库id
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long warehouseSkuId);

    /**
     * 根据指定的仓库分配策略扣减库存
     *
     * @param warehouses 仓库及发货数量列表
     * @return 是否扣减成功
     */
    Response<Boolean> decreaseStock(List<SelectedWarehouse> warehouses);
}