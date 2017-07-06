package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述Dao类
 * Date: 2017-06-07
 */
@Repository
public class WarehouseRuleDao extends MyBatisDao<WarehouseRule> {

    /**
     * 根据店铺组id查找对应的规则
     *
     * @param shopGroupId 店铺组id
     * @return 对应的规则列表
     */
    public List<WarehouseRule> findByShopGroupId(Long shopGroupId){
        return getSqlSession().selectList(sqlId("findByShopGroupId"), shopGroupId);
    }

    /**
     * 根据店铺组id列表查找对应的规则
     *
     * @param shopGroupIds 店铺组id列表
     * @return 对应的规则列表
     */
    public List<WarehouseRule> findByShopGroupIds(List<Long> shopGroupIds){
        return getSqlSession().selectList(sqlId("findByShopGroupIds"), shopGroupIds);
    }

    /**
     * 这里是根据shopGroupId 分页
     *
     * @param offset 偏移
     * @param limit 最多返回条数
     * @return 分页记过
     */
    @Override
    public Paging<WarehouseRule> paging(Integer offset, Integer limit) {
        Long count = getSqlSession().selectOne(sqlId("count"));
        if(Objects.equal(count,0L)){
            return Paging.empty();
        }
        List<Long> shopGroupIds = getSqlSession().selectList(sqlId("pagination"),
                ImmutableMap.of("offset",offset, "limit",limit));
        if(CollectionUtils.isEmpty(shopGroupIds)){
            return new Paging<>(count, Collections.emptyList());
        }
        List<WarehouseRule> rules = findByShopGroupIds(shopGroupIds);
        return new Paging<>(count, rules);
    }
}
