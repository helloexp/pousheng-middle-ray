package com.pousheng.middle.group.impl.dao;

import com.pousheng.middle.group.model.ItemRuleGroup;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
@Repository
public class ItemRuleGroupDao extends MyBatisDao<ItemRuleGroup> {

    public Integer deleteByRuleId(Long ruleId) {
        return getSqlSession().delete(sqlId("deleteByRuleId"), ruleId);
    }

    public List<ItemRuleGroup> findByRuleId(Long ruleId) {
        return getSqlSession().selectList(sqlId("findByRuleId"), ruleId);
    }

    public List<ItemRuleGroup> findByGroupId(Long groupId) {
        return getSqlSession().selectList(sqlId("findByGroupId"), groupId);
    }

}
