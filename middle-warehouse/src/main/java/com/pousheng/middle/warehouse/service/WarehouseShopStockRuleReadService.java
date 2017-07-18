package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.Map;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18 10:37:00
 */
public interface WarehouseShopStockRuleReadService {

    /**
     * 查询
     * @param id 根据id查询
     * @return warehouseShopStockRule
     */
    Response<WarehouseShopStockRule> findById(Long id);


    /**
     * 根据店铺id查询对应的库存分配规则
     *
     * @param shopId 店铺id
     * @return 对应的库存分配规则
     */
    Response<WarehouseShopStockRule> findByShopId(Long shopId);

    /**
     * 分页
     * @param pageNo 起始页码
     * @param pageSize 每页返回数量
     * @param criteria 查询参数
     * @return Paging<WarehouseShopStockRule>
     */
    Response<Paging<WarehouseShopStockRule>> paging(Integer pageNo, Integer pageSize, Map<String, Object> criteria);
}