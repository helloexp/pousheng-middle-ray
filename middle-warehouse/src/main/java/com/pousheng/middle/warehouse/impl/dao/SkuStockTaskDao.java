package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Author: songrenfei
 * Desc: sku库存同步任务Dao类
 * Date: 2018-05-24
 */
@Repository
public class SkuStockTaskDao extends MyBatisDao<SkuStockTask> {


    public List<SkuStockTask> findWaiteHandleLimit(){
        return getSqlSession().selectList(sqlId("findWaiteHandleLimit"));
    }

    public boolean updateToHandle(Long id, Date timeOutAt){
        return getSqlSession().update("updateToHandle", ImmutableMap.of("id",id,"timeoutAt",timeOutAt))> 0;
    }

    public boolean updateToHandleBatch(List list){
        return getSqlSession().update("updateToHandleBatch",list)> 0;
    }

    public boolean updateTimeOutHandleTask(){
        return getSqlSession().update("updateTimeOutHandleTask", ImmutableMap.of("timeoutAt", DateTime.now().plusMinutes(10).toDate(),"endAt",new Date()))> 0;

    }

}
