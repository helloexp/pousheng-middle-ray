package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.AutoCompensation;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by penghui on 2018/1/15
 */
@Repository
public class AutoCompensationDao extends MyBatisDao<AutoCompensation> {

    public List<AutoCompensation> findAutoCompensationTask(Integer type,Integer status){
        return getSqlSession().selectList(sqlId("findAutoCompensationTask"), ImmutableMap.of("type",type,"status",status));
    }

}
