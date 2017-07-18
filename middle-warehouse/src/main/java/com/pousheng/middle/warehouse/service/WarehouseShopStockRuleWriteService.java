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
     * @param warehouseShopStockRule
     * @return Boolean
     */
    Response<Long> create(WarehouseShopStockRule warehouseShopStockRule);

    /**
     * 更新
     * @param warehouseShopStockRule
     * @return Boolean
     */
    Response<Boolean> update(WarehouseShopStockRule warehouseShopStockRule);

    /**
     * 删除
     * @param id
     * @return Boolean
     */
    Response<Boolean> delete(Long id);

}