package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * @author: zhaoxiaowei
 * Desc: Dao类
 * Date: 2018-09-04
 */
@Repository
public class WarehouseRulePriorityDao extends MyBatisDao<WarehouseRulePriority> {

    public Boolean checkByName(WarehouseRulePriority warehouseRulePriority) {
        if (warehouseRulePriority.getId() == null) {
            return (long) getSqlSession().selectOne(sqlId("checkByName"),
                    ImmutableMap.of("name", warehouseRulePriority.getName())) == 0L;

        }
        return (long) getSqlSession().selectOne(sqlId("checkByName"),
                ImmutableMap.of("id", warehouseRulePriority.getId(), "name", warehouseRulePriority.getName())) == 0L;

    }


    public Boolean checkTimeRange(WarehouseRulePriority warehouseRulePriority) {
        if (warehouseRulePriority.getId() == null) {
            return (long) getSqlSession().selectOne(sqlId("checkTimeRange"),
                    ImmutableMap.of("startDate", warehouseRulePriority.getStartDate(), "endDate", warehouseRulePriority.getEndDate(), "ruleId", warehouseRulePriority.getRuleId())) == 0L;

        }
        return (long) getSqlSession().selectOne(sqlId("checkTimeRange"),
                ImmutableMap.of("startDate", warehouseRulePriority.getStartDate(), "endDate", warehouseRulePriority.getEndDate(), "ruleId", warehouseRulePriority.getRuleId(), "id", warehouseRulePriority.getId())) == 0L;
    }

}
