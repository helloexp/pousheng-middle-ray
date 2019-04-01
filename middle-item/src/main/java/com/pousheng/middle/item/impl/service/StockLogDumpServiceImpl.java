/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.item.component.BatchIndexTask;
import com.pousheng.middle.item.dto.IndexedStockLog;
import com.pousheng.middle.item.service.SkuTemplateDumpService;
import com.pousheng.middle.item.service.StockLogDumpService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/8/16
 */
@Slf4j
@Component
@RpcProvider
@SuppressWarnings("unused")
public class StockLogDumpServiceImpl implements StockLogDumpService {

    @Autowired
    private BatchIndexTask batchIndexTask;

    @Override
    public Response<Boolean> batchDump(List<IndexedStockLog> logs) {
        try {
            if (CollectionUtils.isEmpty(logs)) {
                return Response.ok();
            }
            batchIndexTask.batchDumpLogs(logs);
            return Response.ok();
        } catch (Exception e) {
            log.error("batch dump stock log :{} fail,cause:{}", logs, Throwables.getStackTraceAsString(e));
            return Response.fail("batch.dump.fail");
        }
    }
}
