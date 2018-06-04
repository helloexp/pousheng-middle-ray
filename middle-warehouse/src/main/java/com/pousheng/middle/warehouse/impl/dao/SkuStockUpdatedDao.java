package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.SkuStockUpdated;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: lyj
 * Desc: sku库存同步任务临时表Dao类
 * Date: 2018-05-29
 */
@Repository
public class SkuStockUpdatedDao extends MyBatisDao<SkuStockUpdated> {
    public List<String> findWaiteHandle(){
        return getSqlSession().selectList(sqlId("findWaiteHandle"));
    }
    public boolean deleteAll(){
        return getSqlSession().delete(sqlId("deleteAll"))>=0;
    }

/*
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
    }*/
}
