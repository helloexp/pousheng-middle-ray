package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.model.Response;

/**
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
public interface WarehouseAddressReadService {
    /**
     *
     * @param addressName
     * @param level
     * @return
     */
    Response<WarehouseAddress> findByNameAndLevel(String addressName,Integer level);
}
