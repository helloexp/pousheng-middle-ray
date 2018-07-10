package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.impl.dao.PoushengCompensateBizDao;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

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

    @Override
    public Response<List<PoushengCompensateBiz>> findByIdsAndStatus(List<Long> ids, String status) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("find biz task by ids:{}, status: {}", ids, status);
            }
            List<PoushengCompensateBiz> result = poushengCompensateBizDao.findByIdsAndStatus(ids, status);
            if (log.isDebugEnabled()) {
                log.debug("find biz task by ids:{}, status: {}, res: {}", ids, status, result);
            }
            // 查询状态PROCESSING的任务为空，检查任务状态
            if (CollectionUtils.isEmpty(result)) {
                Response<PoushengCompensateBiz> response = this.findById(ids.get(0));
                if (response.isSuccess()) {
                    if (log.isDebugEnabled()) {
                        log.debug("find empty PROCESSING task, first task: {}", response.getResult());
                    }
                }
            }
            return Response.ok(result);
        } catch (Exception e) {
            log.error("find poushengCompensateBiz by ids :{} failed,  cause:{}", ids, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.find.fail");
        }
    }
}
