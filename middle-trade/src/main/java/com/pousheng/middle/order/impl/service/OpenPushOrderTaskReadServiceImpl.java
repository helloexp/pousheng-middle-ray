package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.OpenPushOrderTaskDao;
import com.pousheng.middle.order.model.OpenPushOrderTask;
import com.pousheng.middle.order.service.OpenPushOrderTaskReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/22
 * pousheng-middle
 */
@Slf4j
@Service
public class OpenPushOrderTaskReadServiceImpl implements OpenPushOrderTaskReadService {
    @Autowired
    private OpenPushOrderTaskDao openPushOrderTaskDao;
    @Override
    public Response<List<OpenPushOrderTask>> findByStatus(int status) {
        try {
            return io.terminus.common.model.Response.ok(openPushOrderTaskDao.findByStatus(status));
        } catch (Exception e) {
            log.error("find openPushOrderTask by status :{} failed,  cause:{}", status, Throwables.getStackTraceAsString(e));
            return io.terminus.common.model.Response.fail("openPushOrderTask.find.fail");
        }
    }
}
