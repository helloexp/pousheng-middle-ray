package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.OperationLogDao;
import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.service.OperationLogWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: sunbo
 * Desc: 写服务实现类
 * Date: 2017-07-31
 */
@Slf4j
@Service
public class OperationLogWriteServiceImpl implements OperationLogWriteService {

    private final OperationLogDao operationLogDao;

    @Autowired
    public OperationLogWriteServiceImpl(OperationLogDao operationLogDao) {
        this.operationLogDao = operationLogDao;
    }

    @Override
    public Response<Long> create(OperationLog operationLog) {
        try {
            operationLogDao.create(operationLog);
            return Response.ok(operationLog.getId());
        } catch (Exception e) {
            log.error("create operationLog failed, operationLog:{}, cause:{}", operationLog, Throwables.getStackTraceAsString(e));
            return Response.fail("operation.log.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(OperationLog operationLog) {
        try {
            return Response.ok(operationLogDao.update(operationLog));
        } catch (Exception e) {
            log.error("update operationLog failed, operationLog:{}, cause:{}", operationLog, Throwables.getStackTraceAsString(e));
            return Response.fail("operation.log.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long operationLogId) {
        try {
            return Response.ok(operationLogDao.delete(operationLogId));
        } catch (Exception e) {
            log.error("delete operationLog failed, operationLogId:{}, cause:{}", operationLogId, Throwables.getStackTraceAsString(e));
            return Response.fail("operation.log.delete.fail");
        }
    }
}
