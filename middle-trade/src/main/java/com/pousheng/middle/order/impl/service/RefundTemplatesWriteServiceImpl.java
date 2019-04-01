package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.RefundTemplatesDao;
import com.pousheng.middle.order.model.Refundtemplates;
import com.pousheng.middle.order.service.RefundTemplatesWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RefundTemplatesWriteServiceImpl implements RefundTemplatesWriteService {

    @Autowired
    private RefundTemplatesDao refundTemplatesDao;

    @Override
    public Response<String> creates(List<Refundtemplates> reftemplates) {
        try {
            refundTemplatesDao.creates(reftemplates);
            return Response.ok(reftemplates.get(0).getBatchCode());
        } catch (Exception e){
            log.error("Refundtemplates created failed, BatchCode:{}, cause:{}", reftemplates.get(0).getBatchCode(), Throwables.getStackTraceAsString(e));
            return Response.fail("Refundtemplates.created.fail");
        }
        
    }

    @Override
    public Response<Boolean> updateApplyStatusByid(Long id) {
        try {
            return Response.ok(refundTemplatesDao.updateApplyStatusByid(id));
        }catch (Exception e){
            log.error("update RefundTemplates failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("ApplyStatus.update.fail");
        }        
    }
}
