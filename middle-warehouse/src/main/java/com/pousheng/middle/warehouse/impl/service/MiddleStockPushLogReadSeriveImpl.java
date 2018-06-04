package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.StockPushLogDao;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogReadSerive;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
@Slf4j
@Service
public class MiddleStockPushLogReadSeriveImpl implements MiddleStockPushLogReadSerive
{
    @Autowired
    private StockPushLogDao stockPushLogDao;

    @Override
    public Response<StockPushLog> findById(Long Id) {
        try {
            return Response.ok(stockPushLogDao.findById(Id));
        } catch (Exception e) {
            log.error("find stockPushLog by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("stockPushLog.find.fail");
        }
    }

    @Override
    public Response<Paging<StockPushLog>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params) {
        try{
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<StockPushLog> p = stockPushLogDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), params);
            return Response.ok(p);
        }catch (Exception e){
            log.error("failed to pagination stockPushLog with params:{}, cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            return Response.fail("stockPushLog.find.fail");
        }
    }
}
