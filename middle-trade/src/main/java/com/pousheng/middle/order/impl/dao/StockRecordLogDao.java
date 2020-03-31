package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.enums.StockRecordType;
import com.pousheng.middle.order.model.StockRecordLog;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/14
 * Time: 下午12:10
 */
@Repository
public class StockRecordLogDao extends MyBatisDao<StockRecordLog> {

    public List<StockRecordLog> findRejectHistoryOfThreeDay(String skuCode, List<Long> warehouseIds) {
        Map<String, Object> criteria = Maps.newHashMap();
        criteria.put("skuCode", skuCode);
        criteria.put("warehouseIds", warehouseIds);
        criteria.put("type", StockRecordType.MPOS_REFUSE_ORDER);
        return getSqlSession().selectList(sqlId("findRejectHistoryOfThreeDay"), criteria);
    }
}
