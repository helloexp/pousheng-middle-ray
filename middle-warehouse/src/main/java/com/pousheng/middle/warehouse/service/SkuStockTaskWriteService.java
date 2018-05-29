package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.SkuStockTask;
import io.terminus.common.model.Response;

/**
 * Author: songrenfei
 * Desc: sku库存同步任务写服务
 * Date: 2018-05-24
 */

public interface SkuStockTaskWriteService {

    /**
     * 创建SkuStockTask
     * @param skuStockTask 任务
     * @return 主键id
     */
    Response<Long> create(SkuStockTask skuStockTask);

    /**
     * 更新SkuStockTask
     * @param skuStockTask 任务
     * @return 是否成功
     */
    Response<Boolean> update(SkuStockTask skuStockTask);

    /**
     * 根据主键id删除SkuStockTask
     * @param skuStockTaskId 任务
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long skuStockTaskId);

    /**
     * 更新超时时间还处理中还未处理完的任务状态回滚到待处理
     * @return 是否更新成功
     */
    Response<Boolean> updateTimeOutHandleTask();

    /**
     * 根据主键id更新SkuStockTask状态
     * @param skuStockTaskId 任务
     * @return 是否成功
     */
    Response<Boolean> updateStatusById(Long skuStockTaskId,int status);


}