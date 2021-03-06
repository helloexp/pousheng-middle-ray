package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.impl.dao.PoushengCompensateBizDao;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public Response<Boolean> updateStatusByContextInTwoHours(String context, String currentStatus, String newStatus,
            String bizType) {
        try {
            boolean result = poushengCompensateBizDao.updateStatusByContext(context, currentStatus, newStatus, bizType);
            if (result) {
                return Response.ok(Boolean.TRUE);
            } else {
                return Response.fail("update.biz.status.failed");
            }
        } catch (Exception e) {
            log.error("update poushengCompensateBiz status to {} failed, id:{},currentStatus is {}, cause:{}",
                    newStatus, context, currentStatus, Throwables.getStackTraceAsString(e));
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

    @Override
    public Response<Boolean> batchUpdateStatus(List<Long> ids, String status) {
        try {
            poushengCompensateBizDao.batchUpdateStatus(ids, status);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("batch update poushengCompensateBiz status to {} failed, ids:{}, cause:{}",status, ids, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.update.status.fail");
        }
    }

    @Override
    public Response<Boolean> resetStatus() {
        try {
            poushengCompensateBizDao.resetStatus();
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("reset biz compensation task failed,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("reset.compensation.status.fail");
        }
    }

    @Override
    public Response<Long> create(String bizType, String context,String bizId) {
        PoushengCompensateBiz poushengCompensateBiz = new PoushengCompensateBiz();
        try {
            poushengCompensateBiz.setContext(context);
            poushengCompensateBiz.setBizType(bizType);
            poushengCompensateBiz.setBizId(bizId);
            poushengCompensateBiz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
            poushengCompensateBiz.setCnt(0);
            poushengCompensateBizDao.create(poushengCompensateBiz);
            return Response.ok(poushengCompensateBiz.getId());
        } catch (Exception e) {
            log.error("failed to create poushengCompensateBiz. bizType:{},context:{}", bizType, context, e);
            return Response.fail("poushengCompensateBiz.create.fail");
        }
    }

    @Override
    public Response<String> updateBizTypeByContextOnlyIfOfWaitHandleStatus(Long id, String currentBizType,
            String newBizType) {
        try {
            boolean result = poushengCompensateBizDao.updateTypeByContextOnlyIfOfWaitHandleStatus(String.valueOf(id),
                    currentBizType,
                    newBizType);
            if (result) {
                return Response.ok("ok");
            } else {
                return Response.fail("update.biz.type.failed");
            }
        } catch (Exception e) {
            log.error("update poushengCompensateBiz type to {} failed, id:{},currentBizType is {}, cause:{}",
                    newBizType, id, currentBizType, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengCompensateBiz.update.type.fail");
        }
    }
}
