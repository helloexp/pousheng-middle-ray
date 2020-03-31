package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.OperationLogCriteria;
import com.pousheng.middle.order.impl.dao.OperationLogDao;
import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.service.OperationLogReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: sunbo
 * Desc: 读服务实现类
 * Date: 2017-07-31
 */
@Slf4j
@Service
public class OperationLogReadServiceImpl implements OperationLogReadService {

    private final OperationLogDao operationLogDao;

    @Autowired
    public OperationLogReadServiceImpl(OperationLogDao operationLogDao) {
        this.operationLogDao = operationLogDao;
    }

    @Override
    public Response<OperationLog> findById(Long Id) {
        try {
            return Response.ok(operationLogDao.findById(Id));
        } catch (Exception e) {
            log.error("find operationLog by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("operation.log.find.fail");
        }
    }

    @Override
    public Response<Paging<OperationLog>> paging(OperationLogCriteria criteria) {
        try {
            Paging<OperationLog> paging = operationLogDao.paging(criteria.getOffset(),criteria.getLimit(),criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging operation log, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("operation.log.find.fail");
        }
    }
}
