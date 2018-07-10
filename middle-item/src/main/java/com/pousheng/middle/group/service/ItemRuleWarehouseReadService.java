package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemRuleWarehouse;
import com.pousheng.middle.group.model.ItemRuleWarehouse;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 商品规则与仓库关系映射表读服务
 * Date: 2018-07-13
 */

public interface ItemRuleWarehouseReadService {

    /**
     * 根据id查询商品规则与仓库关系映射表
     *
     * @param Id 主键id
     * @return 商品规则与仓库关系映射表
     */
    Response<ItemRuleWarehouse> findById(Long Id);

    /**
     * 根据id查询
     * @param ruleId 规则id
     * @return
     */
    Response<List<ItemRuleWarehouse>> findByRuleId(Long ruleId);

    /**
     * 根据id查询
     * @return
     */
    Response<List<Long>> findWarehouseIds();

    /**
     * 检查店铺分组情况
     * @param ruleId 分组id
     * @param warehouseIds  店铺id
     * @return
     */
    Response<Boolean>  checkWarehouseIds(Long ruleId,List<Long> warehouseIds);

    /**
     * 根据店铺id查询所属规则Id
     * @param warehouseId 店铺id
     * @return
     */
    Response<Long>  findRuleIdByWarehouseId(Long warehouseId);
}
