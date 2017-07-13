package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.RuleSummary;
import com.pousheng.middle.warehouse.dto.ThinAddress;
import com.pousheng.middle.warehouse.dto.Warehouses4Address;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联读服务
 * Date: 2017-06-07
 */

public interface WarehouseAddressRuleReadService {

    /**
     * 根据规则id查询地址和仓库规则的关联
     *
     * @param ruleId 规则id
     * @return 规则概述
     */
    Response<RuleSummary> findByRuleId(Long ruleId);

    /**
     * 根据仓库优先级规则id, 返回对应的仓库发货地址信息
     *
     * @param ruleId 规则id
     * @return 仓库发货地址信息
     */
    Response<List<ThinAddress>> findAddressByRuleId(Long ruleId);

    /**
     * 查找店铺组其他规则用掉的非默认地址
     *
     * @param ruleId ruleId
     * @return 对应的仓库发货地址集合
     */
    Response<List<ThinAddress>> findOtherNonDefaultAddressesByRuleId(Long ruleId);


    /**
     * 查找店铺组中发货规则已经用掉的地址
     *
     * @param shopGroupId 店铺组id
     * @return 对应的仓库发货地址集合
     */
    Response<List<ThinAddress>> findNonDefaultAddressesByShopGroupId(Long shopGroupId);



    /**
     * 根据层级地址, 返回满足条件的仓库, 最精确的地址优先
     *
     * @param shopId 店铺id
     * @param addressIds 收货地址, 最精确的地址放在第一个,比如按照[区, 市, 省, 全国]的顺序传入
     * @return 所有能够发货到该地址的仓库列表
     */
    Response<List<Warehouses4Address>> findByReceiverAddressIds(Long shopId, List<Long> addressIds);
}
