package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.Map;

/**
 * Author: jlchen
 * Desc: 仓库读服务
 * Date: 2017-06-07
 */

public interface WarehouseReadService {

    /**
     * 根据id查询仓库
     * @param Id 主键id
     * @return 仓库
     */
    Response<Warehouse> findById(Long Id);

    /**
     * 仓库列表
     *
     * @param pageNo 起始页码
     * @param pageSize 每页返回数目
     * @param params 其他查询参数
     * @return 仓库列表
     */
    Response<Paging<Warehouse>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params);
}
