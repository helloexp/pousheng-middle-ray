package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.MposSkuStock;
import io.terminus.common.model.Response;

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
}