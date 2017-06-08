package com.pousheng.middle.warehouse.impl.dao;

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
}
