package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.StockRecordLogDao;
import com.pousheng.middle.order.model.StockRecordLog;
import com.pousheng.middle.order.service.StockRecordLogWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/14
 * Time: 下午12:13
 */
@Slf4j
@Service
public class StockRecordLogWriteServiceImpl implements StockRecordLogWriteService {

    @Autowired
    private StockRecordLogDao stockRecordLogDao;

    @Override
    public Response<Long> create(StockRecordLog stockRecordLog) {
        try {
            stockRecordLogDao.create(stockRecordLog);
            return Response.ok(stockRecordLog.getId());
        } catch (Exception e) {
            log.error("create stock log failed, stockRecordLog:{}, cause:{}", stockRecordLog, Throwables.getStackTraceAsString(e));
            return Response.fail("stock.log.create.fail");
        }
    }
}
