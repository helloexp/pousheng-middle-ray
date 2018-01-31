package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.OpenPushOrderTaskDao;
import com.pousheng.middle.order.model.OpenPushOrderTask;
import com.pousheng.middle.order.service.OpenPushOrderTaskWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/22
 * pousheng-middle
 * @author tony
 */
@Slf4j
@Service
public class OpenPushOrderTaskWriteServiceImpl implements OpenPushOrderTaskWriteService {
    @Autowired
    private OpenPushOrderTaskDao openPushOrderTaskDao;
    @Override
    public Response<Long> create(OpenPushOrderTask openPushOrderTask) {
        try {
            openPushOrderTaskDao.create(openPushOrderTask);
            return Response.ok(openPushOrderTask.getId());
        } catch (Exception e) {
            log.error("create openPushOrderTask failed, openPushOrderTask:{}, cause:{}", openPushOrderTask, Throwables.getStackTraceAsString(e));
            return Response.fail("openPushOrderTask.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(OpenPushOrderTask openPushOrderTask) {
        try {
            return Response.ok(openPushOrderTaskDao.update(openPushOrderTask));
        } catch (Exception e) {
            log.error("update openPushOrderTask failed, openPushOrderTask:{}, cause:{}", openPushOrderTask, Throwables.getStackTraceAsString(e));
            return Response.fail("openPushOrderTask.update.fail");
        }
    }
}
