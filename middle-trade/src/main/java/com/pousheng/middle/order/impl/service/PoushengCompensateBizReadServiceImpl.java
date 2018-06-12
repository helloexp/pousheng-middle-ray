package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.impl.dao.PoushengCompensateBizDao;
import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@Slf4j
@Service
public class PoushengCompensateBizReadServiceImpl implements PoushengCompensateBizReadService {
    @Autowired
    private PoushengCompensateBizDao poushengCompensateBizDao;
    @Override
    public Response<PoushengCompensateBiz> findById(Long Id) {
        try {
            return Response.ok(poushengCompensateBizDao.findById(Id));
        } catch (Exception e) {
            log.error("find poushengCompensateBiz by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.find.fail");
        }
    }

    @Override
    public Response<Paging<PoushengCompensateBiz>> paging(PoushengCompensateBizCriteria criteria) {
        try {
            Paging<PoushengCompensateBiz> paging = poushengCompensateBizDao.paging(criteria.getOffset(),criteria.getLimit(),criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging poushengCompensateBiz, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.poushengCompensateBiz.find.fail");
        }
    }
}
