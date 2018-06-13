package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.PoushengCompensateBizDao;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 * @author tony
 */
@Slf4j
@Service
public class PoushengCompensateBizWriteServiceImpl implements PoushengCompensateBizWriteService {
    @Autowired
    private PoushengCompensateBizDao poushengCompensateBizDao;
    @Override
    public Response<Long> create(PoushengCompensateBiz poushengCompensateBiz) {
        try {
            poushengCompensateBiz.setCnt(0);
            poushengCompensateBizDao.create(poushengCompensateBiz);
            return Response.ok(poushengCompensateBiz.getId());
        } catch (Exception e) {
            log.error("create poushengCompensateBiz failed, poushengCompensateBiz:{}, cause:{}", poushengCompensateBiz, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(PoushengCompensateBiz poushengCompensateBiz) {
        try {
            boolean result = poushengCompensateBizDao.update(poushengCompensateBiz);
            if (result){
                return Response.ok(Boolean.TRUE);
            }else{
                return Response.fail("update.biz.failed");
            }
        } catch (Exception e) {
            log.error("update poushengCompensateBiz failed, poushengCompensateBiz:{}, cause:{}", poushengCompensateBiz, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long id) {
        try {
            boolean result = poushengCompensateBizDao.delete(id);
            if (result){
                return Response.ok(Boolean.TRUE);
            }else{
                return Response.fail("delete.biz.failed");
            }
        } catch (Exception e) {
            log.error("delete poushengCompensateBiz failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.delete.fail");
        }
    }

    @Override
    public Response<Boolean> updateStatus(Long id, String currentStatus, String newStatus) {
        try {
            boolean result = poushengCompensateBizDao.updateStatus(id,currentStatus,newStatus);
            if (result){
                return Response.ok(Boolean.TRUE);
            }else{
                return Response.fail("update.biz.status.failed");
            }
        } catch (Exception e) {
            log.error("update poushengCompensateBiz status to {} failed, id:{},currentStatus is {}, cause:{}",newStatus, id,currentStatus, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.update.status.fail");
        }
    }

    @Override
    public Response<Boolean> updateLastFailedReason(Long id, String lastFailedReason,Integer cnt) {
        try {
            PoushengCompensateBiz biz = new PoushengCompensateBiz();
            biz.setId(id);
            biz.setCnt(cnt);
            biz.setLastFailedReason(lastFailedReason);
            boolean result = poushengCompensateBizDao.update(biz);
            if (result){
                return Response.ok(Boolean.TRUE);
            }else{
                return Response.fail("update.biz.reason.failed");
            }
        } catch (Exception e) {
            log.error("update poushengCompensateBiz failed, id:{},lastFailedReason:{}, cause:{}", id,lastFailedReason, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.update.fail");
        }
    }
}
