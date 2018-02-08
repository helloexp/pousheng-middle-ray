package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/21
 * pousheng-middle
 * @author tony
 */
@Repository
public class PoushengSettlementPosDao extends MyBatisDao<PoushengSettlementPos>{
    public PoushengSettlementPos findByPosSerialNo(String posSerialNo){
        return getSqlSession().selectOne(sqlId("findByPosSerialNo"), posSerialNo);
    }
    public PoushengSettlementPos findByShipmentId(Long shipmentId){
        return getSqlSession().selectOne(sqlId("findByShipmentId"), shipmentId);
    }
    public PoushengSettlementPos findByRefundIdAndPosType(Long orderId,Integer posType){
        return getSqlSession().selectOne(sqlId("findByRefundIdAndPosType"), ImmutableMap.of("orderId", orderId, "posType", posType));
    }
}
