package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.StockRecordLog;
import io.terminus.common.model.Response;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/14
 * Time: 下午12:11
 */
public interface StockRecordLogWriteService {

    /**
     * 新增库存查询日志
     * @param stockRecordLog
     * @return
     */
    Response<Long> create(StockRecordLog stockRecordLog);
}
