package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.model.SkuStockUpdated;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;

import java.util.List;
import java.util.Map;

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
