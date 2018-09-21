package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.StockPushLog;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
@Repository
public class StockPushLogDao extends MyBatisDao<StockPushLog> {

    public int batchUpdateSucessByRequestIdAndLineNo(String requestNo,List<String> lineNos) {
        return getSqlSession().update(sqlId("batchUpdateSuccessByRequestIdAndLineNo"), ImmutableMap.of("requestNo",requestNo,"lineNos",lineNos));
    }
    public int batchUpdateFailureByRequestIdAndLineNo(List<StockPushLog> stockPushLogs) {
        return getSqlSession().update(sqlId("batchUpdateFailureByRequestIdAndLineNo"), stockPushLogs);
    }

    public int updateStatusByRequest(StockPushLog stockPushLog) {
        return getSqlSession().update(sqlId("updateStatusByRequest"), stockPushLog);
    }

    public int deleteByBeforeDate(Date date){
        return getSqlSession().delete(sqlId("deleteByBeforeDate"), date);
    }

}
