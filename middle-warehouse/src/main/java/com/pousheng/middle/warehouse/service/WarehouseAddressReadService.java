package com.pousheng.middle.warehouse.service;

import com.google.common.base.Optional;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.model.Response;

/**
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
public interface WarehouseAddressReadService {
    /**
     * 根据名称 和 级别查询地址信息
     * @param addressName 地址名称
     * @param level 级别
     * @return 地址
     */
    Response<WarehouseAddress> findByNameAndLevel(String addressName,Integer level);

    /**
     * 根据名称查询地址信息
     * @param addressName 地址名称
     * @return 地址信息
     */
    Response<Optional<WarehouseAddress>> findByName(String addressName);
}
