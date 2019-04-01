package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.Refundtemplates;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RefundTemplatesDao extends MyBatisDao<Refundtemplates> {
    
    public List<Refundtemplates> getRefundTemplatesListBycode(String batchCode){
        return getSqlSession().selectList(sqlId("getRefundTemplatesListBycode"),ImmutableMap.of("batchCode",batchCode));
    }

    public boolean updateApplyStatusByid(Long id){
        return getSqlSession().update(sqlId("updateApplyStatus"), ImmutableMap.of("id",id))==1;
    }
}
