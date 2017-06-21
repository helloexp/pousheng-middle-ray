package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库读服务
 * Date: 2017-06-21
 */

public interface WarehouseShopReturnReadService {

    /**
     * 根据id查询店铺的退货仓库
     * @param Id 主键id
     * @return 店铺的退货仓库
     */
    Response<WarehouseShopReturn> findById(Long Id);
}
