package com.pousheng.middle.item.service;

import com.pousheng.middle.item.dto.IndexedStockLog;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/8/16
 */
public interface StockLogDumpService {

    /**
     * 批量插入es
     * @param logs 日志记录
     * @return 是否dump成功
     */
    Response<Boolean> batchDump(List<IndexedStockLog> logs);
}
