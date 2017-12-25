package com.pousheng.middle.warehouse.service;

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
}
