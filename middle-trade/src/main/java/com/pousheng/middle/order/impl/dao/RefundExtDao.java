package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.RefundExt;
import com.pousheng.middle.order.model.ShopOrderExt;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@Repository
public class RefundExtDao extends MyBatisDao<RefundExt> {

    public boolean updateTradeNo(Long id, String originTradeNo, String newTradeNo) {
        return getSqlSession().update(sqlId("updateTradeNo"), ImmutableMap.of("id", id, "originTradeNo", originTradeNo, "newTradeNo", newTradeNo)) == 1;
    }
}
