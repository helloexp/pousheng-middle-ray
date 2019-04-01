package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.RefundTemplatesDao;
import com.pousheng.middle.order.model.Refundtemplates;
import com.pousheng.middle.order.service.RefundTemplatesReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RefundTemplatesReadServiceImpl implements RefundTemplatesReadService {
    
    @Autowired
    private RefundTemplatesDao refundTemplatesDao;

    @Override
    public Response<List<Refundtemplates>> findByBatchCode(String batchcode) {
        try {
            return Response.ok(refundTemplatesDao.getRefundTemplatesListBycode(batchcode));
        }catch (Exception e){
            log.error(" Refundtemplates findByBatchCode error,cause={}", Throwables.getStackTraceAsString(e));
            return Response.fail("Refundtemplates.query.fail");
        }
    }
}
