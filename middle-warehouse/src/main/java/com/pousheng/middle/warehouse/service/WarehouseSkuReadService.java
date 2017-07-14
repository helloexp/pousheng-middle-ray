package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况读服务
 * Date: 2017-06-07
 */

public interface WarehouseSkuReadService {

    /**
     * 根据id查询sku在仓库的库存情况
     * @param Id 主键id
     * @return sku在仓库的库存情况
     */
    Response<WarehouseSkuStock> findById(Long Id);

    /**
     * 查询某个sku在指定仓库的库存情况
     *
     * @param warehouseId 仓库id
     * @param skuCode skuCode
     * @return sku在仓库的库存情况
     */
    Response<WarehouseSkuStock> findByWarehouseIdAndSkuCode(Long warehouseId, String skuCode);

    /**
     * 根据sku编码查找在仓库中的总的可用库存
     *
     * @param skuCode sku编码
     * @return 库存结果
     */
    Response<WarehouseSkuStock>  findAvailStockBySkuCode(String skuCode);

    /**
     * 分页查询 sku的库存概览情况(不分仓)
     *
     * @param pageNo 起始页码
     * @param pageSize 每页返回条数
     * @param params 查询参数
     * @return 分页结果
     */
    Response<Paging<WarehouseSkuStock>> findBy(Integer pageNo, Integer pageSize, Map<String, Object> params);

    /**
     * 批量查询skuCodes在某个仓库中的库存情况
     *
     * @param warehouseId 仓库id
     * @param skuCodes sku codes
     * @return skuCodes在某个仓库中的库存情况
     */
    Response<Map<String,Integer>> findByWarehouseIdAndSkuCodes(Long warehouseId, List<String> skuCodes);
}
