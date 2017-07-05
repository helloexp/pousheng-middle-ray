package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联Dao类
 * Date: 2017-06-07
 */
@Repository
public class WarehouseAddressRuleDao extends MyBatisDao<WarehouseAddressRule> {

    public void deleteByRuleId(Long ruleId) {
        getSqlSession().delete(sqlId("deleteByRuleId"), ruleId);
    }

    public List<WarehouseAddressRule> findByRuleId(Long ruleId) {
        return getSqlSession().selectList(sqlId("findByRuleId"), ruleId);
    }

    /**
     * 匹配能够发货到对应地址的规则
     *
     * @param shopId 店铺id
     * @param addressId 地址id
     * @return 符合条件的规则列表
     */
    public List<WarehouseAddressRule> findByShopIdAndAddressId(Long shopId, Long addressId){
        return getSqlSession().selectList(sqlId("findByShopIdAndAddressId"),
                ImmutableMap.of("shopId", shopId, "addressId", addressId));

    }

    /**
     * 查找店铺其他规则用掉的非默认地址
     *
     * @param shopId 店铺id
     * @param ruleId ruleId
     * @return 对应的仓库发货地址集合
     */
    public List<WarehouseAddressRule> findOtherNonDefaultRuleByShopId(Long shopId, Long ruleId) {
        return getSqlSession().selectList(sqlId("findOtherNonDefaultRuleByShopId"),
                ImmutableMap.of("shopId", shopId, "ruleId", ruleId));
    }
}
