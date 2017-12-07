package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.PoushengSettlementPosDao;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/21
 * pousheng-middle
 */
@Slf4j
@Service
public class PoushengSettlementPosWriteServiceImpl implements PoushengSettlementPosWriteService {
    @Autowired
    private PoushengSettlementPosDao poushengSettlementPosDao;
    @Override
    public Response<Long> create(PoushengSettlementPos poushengSettlementPos) {
        try {
            poushengSettlementPosDao.create(poushengSettlementPos);
            return io.terminus.common.model.Response.ok(poushengSettlementPos.getId());
        } catch (Exception e) {
            log.error("create poushengSettlementPos failed, poushengSettlementPos:{}, cause:{}", poushengSettlementPos, Throwables.getStackTraceAsString(e));
            return Response.fail("pousheng.setttlement.pos.create.fail");
        }
    }
}
