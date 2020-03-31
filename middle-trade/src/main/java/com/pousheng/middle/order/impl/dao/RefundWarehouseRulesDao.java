package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.RefundWarehouseRules;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Author: wenchao.he
 * Desc:
 * Date: 2019/8/26
 */
@Repository
public class RefundWarehouseRulesDao extends MyBatisDao<RefundWarehouseRules> {

    /**
     * 根据销售店铺id和发货仓账套查询
     * @param shopId
     * @param shipmentCompanyId
     * @return
     */
    public RefundWarehouseRules findByShopIdAndShipmentCompanyId(Long shopId,String shipmentCompanyId){
        return getSqlSession().selectOne(sqlId("findByShopIdAndShipmentCompanyId"), ImmutableMap.of("shopId",shopId,"shipmentCompanyId",shipmentCompanyId));
    }
    
}
