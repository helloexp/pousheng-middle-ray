package com.pousheng.middle.warehouse.service;

import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author:  lyj
 * Date: 2017/10/29
 * pousheng-middle
 */
public interface SkuStockUpdatedReadService {
    /**
     * 根据查询待处理的sku库存同步任务
     * @return sku库存同步任务
     */
    Response<List<String>> findWaiteHandle();
}
