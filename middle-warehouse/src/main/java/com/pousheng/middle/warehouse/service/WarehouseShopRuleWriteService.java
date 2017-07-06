package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.ThinShop;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
public interface WarehouseShopRuleWriteService {

    /**
     * 创建店铺到发货规则的映射关系
     *
     * @param thinShops 店铺列表
     * @return 对应的规则id
     */
    Response<Long> batchCreate(List<ThinShop> thinShops);


    /**
     * 更新规则对应的店铺列表
     * @param ruleId 规则id
     * @param shops 店铺列表
     * @return 对应的规则id
     */
    Response<Boolean> batchUpdate(Long ruleId, List<ThinShop> shops);


    /**
     * 根据规则id删除店铺到发货规则的映射关系
     * @param ruleId 规则id
     * @return 是否成功
     */
    Response<Boolean> deleteByShopGroupId(Long ruleId);
}
