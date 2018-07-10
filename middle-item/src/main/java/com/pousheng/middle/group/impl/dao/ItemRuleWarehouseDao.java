package com.pousheng.middle.group.impl.dao;

import com.google.common.collect.Maps;
import com.pousheng.middle.group.model.ItemRuleWarehouse;
import com.pousheng.middle.group.model.ItemRuleWarehouse;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Author: songrenfei
 * Desc: 商品规则与仓库关系映射表Dao类
 * Date: 2018-07-13
 */
@Repository
public class ItemRuleWarehouseDao extends MyBatisDao<ItemRuleWarehouse> {

    public Integer deleteByRuleId(Long ruleId) {
        return getSqlSession().delete(sqlId("deleteByRuleId"), ruleId);
    }

    public Boolean checkWarehouseIds(Long ruleId, List<Long> warehouseIds) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("ruleId", ruleId);
        params.put("warehouseIds", warehouseIds);
        return (Long) getSqlSession().selectOne(sqlId("countByRuleIdAndWarehouseIds"), params) > 0;
    }

    public List<ItemRuleWarehouse> findByRuleId(Long ruleId) {
        return getSqlSession().selectList(sqlId("findByRuleId"), ruleId);
    }

    public List<Long> findWarehouseIds() {
        return getSqlSession().selectList(sqlId("findWarehouseIds"));
    }

    public Long findRuleIdByWarehouseId(Long warehouseId) {
        return getSqlSession().selectOne(sqlId("findRuleIdByWarehouseId"), warehouseId);
    }

}
