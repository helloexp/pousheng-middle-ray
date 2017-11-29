package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.PoushengGiftActivityCriteria;
import com.pousheng.middle.order.impl.dao.PoushengGiftActivityDao;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.order.service.PoushengGiftActivityReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 */
@Slf4j
@Service
public class PoushengGiftActivityReadServiceImpl implements PoushengGiftActivityReadService{
    @Autowired
    private PoushengGiftActivityDao poushengGiftActivityDao;

    @Override
    public Response<Paging<PoushengGiftActivity>> paging(PoushengGiftActivityCriteria criteria) {
        try {
            Paging<PoushengGiftActivity> paging = poushengGiftActivityDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging pousheng gift activity, criteria={}, cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.poushengGiftActivity.find.fail");
        }
    }

    @Override
    public Response<PoushengGiftActivity> findById(Long id) {
        try {
            if (id == null) {
                log.error(" gift activity id is null");
                return Response.fail("poushengGiftActivity.id.null");
            }

            PoushengGiftActivity poushengGiftActivity = poushengGiftActivityDao.findById(id);
            return Response.ok(poushengGiftActivity);
        } catch (Exception e) {
            log.error("failed to pousheng gift activity, id={}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("single.poushengGiftActivity.find.fail");
        }
    }
}
