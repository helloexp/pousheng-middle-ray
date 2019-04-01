package com.pousheng.middle.order.impl.service;


import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.PoushengGiftActivityDao;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.order.service.PoushengGiftActivityWriteService;
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
public class PoushengGiftActivityWriteServiceImpl implements PoushengGiftActivityWriteService{
    @Autowired
    private PoushengGiftActivityDao poushengGiftActivityDao;

    @Override
    public Response<Long> create(PoushengGiftActivity poushengGiftActivity) {
        try {
            poushengGiftActivityDao.create(poushengGiftActivity);
            return Response.ok(poushengGiftActivity.getId());
        } catch (Exception e) {
            log.error("create poushengGiftActivity failed, poushengGiftActivity:{}, cause:{}", poushengGiftActivity, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengGiftActivity.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(PoushengGiftActivity poushengGiftActivity) {
        try {
            return Response.ok(poushengGiftActivityDao.update(poushengGiftActivity));
        } catch (Exception e) {
            log.error("update poushengGiftActivity failed, poushengGiftActivity:{}, cause:{}", poushengGiftActivity, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengGiftActivity.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long id) {
        try {
            if (id == null) {
                log.error("poushengGiftActivity id is null");
                return Response.fail("poushengGiftActivity.id.null");
            }

            return Response.ok(poushengGiftActivityDao.delete(id));
        } catch (Exception e) {
            log.error("delete poushengGiftActivity failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengGiftActivity.delete.fail");
        }
    }
}
