package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.StockPushLogDao;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
@Slf4j
@Service
public class MiddleStockPushLogWriteSeriveImpl implements MiddleStockPushLogWriteService {
    @Autowired
    private StockPushLogDao stockPushLogDao;

    @Override
    public Response<Long> create(StockPushLog stockPushLog) {
        try {

            stockPushLogDao.create(stockPushLog);
            return Response.ok(stockPushLog.getId());
        } catch (Exception e) {
            log.error("create stockPushLogDao failed, stockPushLogDao:{}, cause:{}", stockPushLogDao, Throwables.getStackTraceAsString(e));
            return Response.fail("stockPushLogDao.create.fail");
        }
    }

    @Override
    public Response<Boolean> creates(List<StockPushLog> stockPushLogs) {
        try {
            if (stockPushLogs.isEmpty()) {
                return Response.fail("stockPushLogs.is.null");
            }
            int result = stockPushLogDao.creates(stockPushLogs);
            if (result > 0) {
                return Response.ok(Boolean.TRUE);
            } else {
                return Response.fail("stockPushLogDao.create.fail");
            }
        } catch (Exception e) {
            log.error("batchCreate stockPushLogDao failed, stockPushLogDao:{}, cause:{}", stockPushLogDao, Throwables.getStackTraceAsString(e));
            return Response.fail("stockPushLogDao.batchCreate.fail");
        }

    }

    @Override
    public Response<Boolean> batchUpdateResultByRequestIdAndLineNo(List<StockPushLog> stockPushLogs) {
        try {
            if (stockPushLogs.isEmpty()) {
                return Response.fail("stockPushLogs.is.null");
            }
            String requestNo = stockPushLogs.get(0).getRequestNo();
            List<String> lineNos = stockPushLogs.stream().map(stockPushLog -> stockPushLog.getLineNo()).collect(Collectors.toList());

            int result1 = stockPushLogDao.batchUpdateSucessByRequestIdAndLineNo(requestNo,lineNos);
            int result2 = stockPushLogDao.batchUpdateFailureByRequestIdAndLineNo(stockPushLogs);
            if (result1 > 0 && result2 >0) {
                return Response.ok(Boolean.TRUE);
            } else {
                return Response.fail("stockPushLogDao.update.fail");
            }
        } catch (Exception e) {
            log.error("batch update stockPushLogDao failed, stockPushLogDao:{}, cause:{}", stockPushLogDao, Throwables.getStackTraceAsString(e));
            return Response.fail("stockPushLogDao.batchCreate.fail");
        }
    }

    @Override
    public Response<Boolean> updateStatusByRequest(StockPushLog stockPushLog){
        try {
            int result = stockPushLogDao.updateStatusByRequest(stockPushLog);
            if (result > 0) {
                return Response.ok(Boolean.TRUE);
            } else {
                return Response.fail("stockPushLogDao.update.fail");
            }
        } catch (Exception e) {
            log.error("create stockPushLogDao failed, stockPushLogDao:{}, cause:{}", stockPushLogDao, Throwables.getStackTraceAsString(e));
            return Response.fail("stockPushLogDao.create.fail");
        }
    }


}
