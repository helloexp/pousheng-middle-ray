package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.StockPushLog;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
public interface MiddleStockPushLogWriteService {

    /**
     * 创建stockPushLog
     *
     * @return 主键id
     */
    Response<Long> create(StockPushLog stockPushLog);

    /**
     * 批量创建stockPushLog
     *
     * @param stockPushLogs
     * @return
     */
    Response<Boolean> creates(List<StockPushLog> stockPushLogs);

    /**
     * @Description TODO
     * @Date        2018/9/13
     * @param       stockPushLogs
     * @return
     */
    public Response<Boolean> batchUpdateResultByRequestIdAndLineNo(List<StockPushLog> stockPushLogs);

    /**
     * @Description TODO
     * @Date        2018/9/19
     * @param       stockPushLog
     * @return
     */
    public Response<Boolean> updateStatusByRequest(StockPushLog stockPushLog);
}
