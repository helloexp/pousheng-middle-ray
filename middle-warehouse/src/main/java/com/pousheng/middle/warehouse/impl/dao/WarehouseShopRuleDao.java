package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseShopRule;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
@Repository
public class WarehouseShopRuleDao extends MyBatisDao<WarehouseShopRule> {

    public void deleteByRuleId(Long ruleId) {
        getSqlSession().delete(sqlId("deleteByRuleId"), ruleId);
    }

    public List<WarehouseShopRule> findByRuleId(Long ruleId) {
        return getSqlSession().selectList(sqlId("findByRuleId"), ruleId);
    }

    /**
     * 匹配店铺对应的规则
     *
     * @param shopId 地址id
     * @return 符合条件的规则列表
     */
    public List<WarehouseShopRule> findByShopId(Long shopId){
        return getSqlSession().selectList(sqlId("findByShopId"), shopId);
    }
}
