package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.base.MoreObjects;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
@Repository
public class WarehouseShopGroupDao extends MyBatisDao<WarehouseShopGroup> {

    public void deleteByGroupId(Long ruleId) {
        getSqlSession().delete(sqlId("deleteByGroupId"), ruleId);
    }

    /**
     * 根据店铺组查找对应的店铺
     *
     * @param groupId 店铺组id
     * @return 对应的店铺列表
     */
    public List<WarehouseShopGroup> findByGroupId(Long groupId) {
        return getSqlSession().selectList(sqlId("findByGroupId"), groupId);
    }

    /**
     * 匹配店铺对应的规则
     *
     * @param shopId 地址id
     * @return 符合条件的规则列表
     */
    public List<WarehouseShopGroup> findByShopId(Long shopId){
        return getSqlSession().selectList(sqlId("findByShopId"), shopId);
    }

    /**
     * 获取已设置发货规则的店铺id集合
     *
     * @return 已设置发货规则的店铺id集合
     */
    public List<Long> findDistinctShopIds() {
        return getSqlSession().selectList(sqlId("findDistinctShopIds"));
    }

    /**
     * 找出当前最大的店铺组id
     *
     * @return 当前最大的店铺组id
     */
    public  Long maxGroupId(){
        Long maxId = getSqlSession().selectOne(sqlId("maxGroupId"));
        return MoreObjects.firstNonNull(maxId, 0L);
    }
}
