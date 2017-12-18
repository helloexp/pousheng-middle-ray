package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.StockPushLog;
import io.terminus.common.model.Response;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
public interface MiddleStockPushLogWriteService {

    /**
     * 创建stockPushLog
     * @return 主键id
     */
    Response<Long> create(StockPushLog stockPushLog);

}
