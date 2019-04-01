package com.pousheng.middle.group.impl.dao;

import com.google.common.collect.Maps;
import com.pousheng.middle.group.model.ItemRuleShop;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
@Repository
public class ItemRuleShopDao extends MyBatisDao<ItemRuleShop> {

    public Integer deleteByRuleId(Long ruleId) {
        return getSqlSession().delete(sqlId("deleteByRuleId"), ruleId);
    }

    public Boolean checkShopIds(Long ruleId, List<Long> shopIds) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("ruleId", ruleId);
        params.put("shopIds", shopIds);
        return (Long) getSqlSession().selectOne(sqlId("countByRuleIdAndShopIds"), params) > 0;
    }

    public List<ItemRuleShop> findByRuleId(Long ruleId) {
        return getSqlSession().selectList(sqlId("findByRuleId"), ruleId);
    }

    public List<Long> findShopIds() {
        return getSqlSession().selectList(sqlId("findShopIds"));
    }

    public Long findRuleIdByShopId(Long shopId) {
        return getSqlSession().selectOne(sqlId("findRuleIdByShopId"), shopId);
    }


}
