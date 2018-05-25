package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.SkuStockTask;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: sku库存同步任务读服务
 * Date: 2018-05-24
 */

public interface SkuStockTaskReadService {

    /**
     * 根据id查询sku库存同步任务
     * @param id 主键id
     * @return sku库存同步任务
     */
    Response<SkuStockTask> findById(Long id);


    /**
     * 根据查询待处理的sku库存同步任务
     * @return sku库存同步任务
     */
    Response<List<SkuStockTask>> findWaiteHandleLimit();





}
