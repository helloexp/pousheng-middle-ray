package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.StockPushLogDao;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
@Slf4j
@Service
public class MiddleStockPushLogWriteSeriveImpl implements MiddleStockPushLogWriteService{
    @Autowired
    private StockPushLogDao stockPushLogDao;

    @Override
    public Response<Long> create(StockPushLog stockPushLog) {
        try {

            stockPushLogDao.create(stockPushLog);
            return Response.ok(stockPushLog.getId());
        } catch (Exception e) {
            log.error("batchCreate stockPushLogDao failed, stockPushLogDao:{}, cause:{}", stockPushLogDao, Throwables.getStackTraceAsString(e));
            return Response.fail("stockPushLogDao.batchCreate.fail");
        }
    }
}
