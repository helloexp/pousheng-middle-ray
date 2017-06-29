package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.ExpressCodeDao;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Created by tony on 2017/6/28.
 * pousheng-middle
 */
@Slf4j
@Service
public class ExpressCodeWriteServiceImpl implements ExpressCodeWriteService {
    @Autowired
    private ExpressCodeDao expressCodeDao;


    @Override
    public Response<Long> create(ExpressCode expressCode) {
        try {
            if (StringUtils.hasText(expressCode.getExpressName())) {
                ExpressCode exist = expressCodeDao.findByExpressName(expressCode.getExpressName());
                if (exist != null) {
                    log.error("duplicated expressName({}) with existed(exprssName={})", expressCode.getExpressName(), exist.getExpressName());
                    return Response.fail("expressCode.expressName.duplicate");
                }
            }
            expressCodeDao.create(expressCode);
            return Response.ok(expressCode.getId());
        } catch (Exception e) {
            log.error("create expressCode failed, expressCode:{}, cause:{}", expressCode, Throwables.getStackTraceAsString(e));
            return Response.fail("expressCode.create.fail");
        }

    }

    @Override
    public Response<Boolean> update(ExpressCode expressCode) {
        try {
            if (expressCode.getId() == null) {
                log.error("exprsssCode id is null");
                return Response.fail("expressCode.id.null");
            }
            return Response.ok(expressCodeDao.update(expressCode));
        } catch (Exception e) {
            log.error("update expressCode failed, expressCode:{}, cause:{}", expressCode, Throwables.getStackTraceAsString(e));
            return Response.fail("expressCode.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long expressCodeId) {
        try {
            if (expressCodeId == null) {
                log.error("invalid exprsssCode id is null");
                return Response.fail("expressCode.id.null");
            }

            return Response.ok(expressCodeDao.delete(expressCodeId));
        } catch (Exception e) {
            log.error("delete expressCode failed, expressCodeId:{}, cause:{}", expressCodeId, Throwables.getStackTraceAsString(e));
            return Response.fail("expressCode.delete.fail");
        }
    }
}
