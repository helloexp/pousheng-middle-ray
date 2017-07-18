package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;

import io.terminus.common.model.Response;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18 10:37:00
 */
public interface WarehouseShopStockRuleWriteService {

    /**
     * 创建
     * @param warehouseShopStockRule  店铺库存分配规则
     * @return  新创建的规则id
     */
    Response<Long> create(WarehouseShopStockRule warehouseShopStockRule);

    /**
     * 更新
     * @param warehouseShopStockRule 店铺库存分配规则
     * @return 是否更新成功
     */
    Response<Boolean> update(WarehouseShopStockRule warehouseShopStockRule);

    /**
     * 删除
     * @param id 要删除分配规则的id
     * @return  是否删除成功
     */
    Response<Boolean> delete(Long id);

}