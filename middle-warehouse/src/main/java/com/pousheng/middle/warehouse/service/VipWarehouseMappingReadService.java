package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.VipWarehouseMapping;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: zhaoxiaowei
 * Desc: 读服务
 * Date: 2018-09-29
 */

public interface VipWarehouseMappingReadService {

    /**
     * 根据id查询
     *
     * @param Id 主键id
     * @return
     */
    Response<VipWarehouseMapping> findById(Long Id);


    /**
     * 根据仓库id查询
     *
     * @param warehouseId
     * @return
     */
    Response<VipWarehouseMapping> findByWarehouseId(Long warehouseId);


    /**
     * 查询映射仓库列表
     *
     * @return
     */
    Response<List<Long>> findAllWarehouseIds();


}
