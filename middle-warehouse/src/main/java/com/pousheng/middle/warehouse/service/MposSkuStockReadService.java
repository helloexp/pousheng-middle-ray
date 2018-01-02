package com.pousheng.middle.warehouse.service;

import com.google.common.base.Optional;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import io.terminus.common.model.Response;

/**
 * Author: songrenfei
 * Desc: mpos下单sku锁定库存情况读服务
 * Date: 2017-12-23
 */

public interface MposSkuStockReadService {

    /**
     * 根据id查询mpos下单sku锁定库存情况
     * @param Id 主键id
     * @return mpos下单sku锁定库存情况
     */
    Response<MposSkuStock> findById(Long Id);

    /**
     * 获取mpos 仓库商品锁定数量
     * @param warehouseId 仓库id
     * @param skuCode 商品编码
     * @return 商品锁定
     */
    Response<Optional<MposSkuStock>> findByWarehouseIdAndSkuCode(Long warehouseId, String skuCode);


    /**
     * 获取mpos 门店商品锁定数量
     * @param shopId 门店id
     * @param skuCode 商品编码
     * @return 商品锁定
     */
    Response<Optional<MposSkuStock>> findByShopIdAndSkuCode(Long shopId, String skuCode);
}
