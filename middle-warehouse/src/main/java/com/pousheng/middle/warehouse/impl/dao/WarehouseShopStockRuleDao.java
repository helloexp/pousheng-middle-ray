package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import io.terminus.common.mysql.dao.MyBatisDao;

import org.springframework.stereotype.Repository;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18 10:37:00
 */
@Repository
public class WarehouseShopStockRuleDao extends MyBatisDao<WarehouseShopStockRule> {

    /**
     * 根据店铺id查找对应的分配规则
     *
     * @param shopId 店铺id
     * @return 对应的分配规则
     */
    public WarehouseShopStockRule findByShopId(Long shopId){
        return getSqlSession().selectOne(sqlId("findByShopId"), shopId);
    }

}
