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


    public List<SkuStockTask> findWaiteHandleLimit(Integer qty,Integer status){
        return getSqlSession().selectList(sqlId("findWaiteHandleLimit"),ImmutableMap.of("qty",qty,"status",status));
    }


    public boolean updateToHandle(Long id, Date timeOutAt){
        return getSqlSession().update(sqlId("updateToHandle"), ImmutableMap.of("id",id,"timeoutAt",timeOutAt))> 0;
    }

    public boolean updateToHandleBatch(List<Long> ids,Integer fromStatus,Integer toStatus){
        return getSqlSession().update(sqlId("updateToHandleBatch"),ImmutableMap.of("ids",ids,"fromStatus",fromStatus,"toStatus",toStatus)) == ids.size();
    }

    public boolean updateTimeOutHandleTask(){
        return getSqlSession().update(sqlId("updateTimeOutHandleTask"), ImmutableMap.of("timeoutAt", DateTime.now().plusMinutes(10).toDate(),"endAt",new Date()))> 0;

    }

    public Boolean updateStatusById(Long skuStockTaskId, int status) {
        return getSqlSession().update(sqlId("updateStatusById"),ImmutableMap.of("id",skuStockTaskId,"status",status))> 0;
    }
}
