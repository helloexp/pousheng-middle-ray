package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.WarehouseShipment;
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
     * 根据指定的仓库分配策略锁定库存
     *
     * @param warehouseShipments 仓库及发货数量列表
     * @return 是否锁定成功
     */
    Response<Boolean> lockStock(List<WarehouseShipment> warehouseShipments);


    /**
     * 根据实际出库的库存情况来变更库存, 这里需要先恢复原来锁定的仓库明细, 然后再根据实际库存做扣减
     *
     * @param lockedShipments 之前锁定的仓库明细
     * @param actualShipments 实际仓库发货明细
     * @return 是否变更成功
     */
    Response<Boolean> decreaseStock(List<WarehouseShipment> lockedShipments,List<WarehouseShipment> actualShipments);
}