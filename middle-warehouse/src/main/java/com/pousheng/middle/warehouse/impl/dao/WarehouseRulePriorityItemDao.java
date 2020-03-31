package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: zhaoxiaowei
 * Desc: Dao类
 * Date: 2018-09-04
 */
@Repository
public class WarehouseRulePriorityItemDao extends MyBatisDao<WarehouseRulePriorityItem> {

    public WarehouseRulePriorityItem findByEntity(WarehouseRulePriorityItem warehouseRulePriorityItem) {
        return getSqlSession().selectOne(sqlId("findByEntity"),
                ImmutableMap.of("priorityId", warehouseRulePriorityItem.getPriorityId(), "warehouseId", warehouseRulePriorityItem.getWarehouseId()));
    }

    public List<WarehouseRulePriorityItem> findByPriorityId(Long priorityId) {
        return getSqlSession().selectList(sqlId("findByPriorityId"), priorityId);
    }
}
