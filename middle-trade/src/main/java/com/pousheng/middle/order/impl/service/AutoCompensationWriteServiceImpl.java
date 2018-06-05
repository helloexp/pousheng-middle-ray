package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.AutoCompensationDao;
import com.pousheng.middle.order.model.AutoCompensation;
import com.pousheng.middle.order.service.AutoCompensationWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by penghui on 2018/1/15
 */
@Service
@Slf4j
public class AutoCompensationWriteServiceImpl implements AutoCompensationWriteService {

    @Autowired
    private AutoCompensationDao autoCompensationDao;

    @Override
    public Response<Long> create(AutoCompensation autoCompensation) {
        try {
            autoCompensationDao.create(autoCompensation);
            return Response.ok(autoCompensation.getId());
        } catch (Exception e) {
            log.error("create autcompensation task failed, autoCompensation:{}, cause:{}", autoCompensation, Throwables.getStackTraceAsString(e));
            return Response.fail("auto.compensation.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(AutoCompensation autoCompensation) {
        try {
            return Response.ok(autoCompensationDao.update(autoCompensation));
        } catch (Exception e) {
            log.error("update auto compensation failed, autoCompensation:{}, cause:{}", autoCompensation, Throwables.getStackTraceAsString(e));
            return Response.fail("auto.compensation.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateStatus(List<Long> ids, Integer status) {
        try {
            autoCompensationDao.updateStatus(ids, status);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("update auto compensation status failed, ids:{}, cause:{}", ids, Throwables.getStackTraceAsString(e));
            return Response.fail("auto.compensation.update.fail");
        }
    }

    @Override
    public Response<Boolean> resetStatus() {
        try {
            autoCompensationDao.resetStatus();
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("reset auto compensation task failed,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("reset.compensation.status.fail");
        }
    }
}
