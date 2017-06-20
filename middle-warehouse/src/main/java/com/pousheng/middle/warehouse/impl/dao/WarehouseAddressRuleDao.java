package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableList;
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
     * @param addressId 地址id
     * @return 符合条件的规则列表
     */
    public List<WarehouseAddressRule> findByAddressId(Long addressId){
        return getSqlSession().selectList(sqlId("findByAddressId"), addressId);
    }

    /**
     * 查找所有的规则
     *
     * @return  所有的规则
     */
    public List<WarehouseAddressRule> findAllButDefault() {
        return getSqlSession().selectList(sqlId("findAllButDefault"));
    }
}
