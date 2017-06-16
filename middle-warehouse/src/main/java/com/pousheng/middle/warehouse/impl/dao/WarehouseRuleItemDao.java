package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则项Dao类
 * Date: 2017-06-07
 */
@Repository
public class WarehouseRuleItemDao extends MyBatisDao<WarehouseRuleItem> {

    /**
     * 根据ruleId 查找对应的优先级规则项
     *
     * @param ruleId 规则id
     * @return  符合条件的优先级规则项
     */
    public List<WarehouseRuleItem> findByRuleId(Long ruleId) {
        return getSqlSession().selectList(sqlId("findByRuleId"), ruleId);
    }
}
