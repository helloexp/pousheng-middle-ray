package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: mpos下单sku锁定库存情况写服务
 * Date: 2017-12-23
 */

public interface MposSkuStockWriteService {

    /**
     * 创建MposSkuStock
     * @param mposSkuStock
     * @return 主键id
     */
    Response<Long> create(MposSkuStock mposSkuStock);

    /**
     * 更新MposSkuStock
     * @param mposSkuStock
     * @return 是否成功
     */
    Response<Boolean> update(MposSkuStock mposSkuStock);

    /**
     * 根据主键id删除MposSkuStock
     * @param mposSkuStockId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long mposSkuStockId);



    /**
     * 根据指定的仓库分配策略锁定库存
     *
     * @param warehouseShipments 仓库及发货数量列表
     * @return 是否锁定成功
     */
    Response<Boolean> lockStockWarehouse(List<WarehouseShipment> warehouseShipments);


    /**
     * 根据指定的仓库分配策略解锁库存, 当撤销发货单时, 调用这个接口
     *
     * @param warehouseShipments 仓库及解锁数量列表
     * @return 是否解锁成功
     */
    Response<Boolean> unlockStockWarehouse(List<WarehouseShipment> warehouseShipments);


    /**
     * 根据指定的门店锁定库存
     *
     * @param shopShipments 门店及发货数量列表
     * @return 是否锁定成功
     */
    Response<Boolean> lockStockShop(List<ShopShipment> shopShipments);


    /**
     * 根据指定的门店分配策略解锁库存, 当撤销发货单时, 调用这个接口
     *
     * @param shopShipments 门店及发货数量列表
     * @return 是否锁定成功
     */
    Response<Boolean> unLockStockShop(List<ShopShipment> shopShipments);


    /**
     * 根据指定的门店和仓库锁定库存
     *
     * @param shopShipments 门店及发货数量列表
     * @param warehouseShipments 仓库及发货数量列表
     * @return 是否锁定成功
     */
    Response<Boolean> lockStockShopAndWarehouse(List<ShopShipment> shopShipments,List<WarehouseShipment> warehouseShipments);
}