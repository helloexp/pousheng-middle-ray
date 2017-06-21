package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库读服务
 * Date: 2017-06-21
 */

public interface WarehouseShopReturnReadService {

    /**
     * 根据id查询店铺的退货仓库
     * @param id 主键id
     * @return 店铺的退货仓库
     */
    Response<WarehouseShopReturn> findById(Long id);

    /**
     * 分页查找退货仓库
     *
     * @param pageNo 起始页码
     * @param pageSize 每页显示条数
     * @param params 查询参数
     * @return 店铺的退货仓库列表
     */
    Response<Paging<WarehouseShopReturn>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params);
}
