package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.model.ShopOrderExt;

import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.Constants;
import io.terminus.parana.order.model.ShopOrder;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@Repository
public class ShopOrderExtDao extends MyBatisDao<ShopOrderExt> {

    public boolean updateHandleStatus(Long id, String newHandleStatus, String originHandleStatus) {
        return getSqlSession().update(sqlId("updateHandleStatus"), ImmutableMap.of("id", id, "newHandleStatus", newHandleStatus, "originHandleStatus", originHandleStatus)) == 1;
    }

    /**
     * 根据来源批量查询订单信息
     * @param outIds
     * @param outFrom
     * @return
     */
    public List<ShopOrder> findByOutIdsAndOutFrom(List<String> outIds,String outFrom){
        return getSqlSession().selectList(sqlId("findByOutIdsAndOutFrom"),
            ImmutableMap.of("outIds", outIds,
            "outFrom", outFrom));
    }
    
    /**
     * 根據電商同步狀態過濾資料
     * @param dtStart
     * @param dtEnd
     * @return
     */
    public Paging<ShopOrder> findByecpOrderStatus(Map<String, Object> params){
    	
        // get total count
        Long total = sqlSession.selectOne(sqlId("findByecpOrderStatusCount"),params);
        if (total <= 0){
            return new Paging<ShopOrder>(0L, Collections.<ShopOrder>emptyList());
        }
        
        // get data
        List<ShopOrder> datas = getSqlSession().selectList(sqlId("findByecpOrderStatus"),params);
        return new Paging<ShopOrder>(total, datas);
        
        
    }
}
