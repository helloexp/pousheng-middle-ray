package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.impl.dao.ExpressCodeDao;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by tony on 2017/6/28.
 */
@Slf4j
@Service
public class ExpressCodeReadServiceImpl implements ExpressCodeReadService {

    @Autowired
    private ExpressCodeDao expressCodeDao;


    @Override
    public Response<Paging<ExpressCode>> pagingExpressCode(ExpressCodeCriteria criteria) {
        try {
            Paging<ExpressCode> paging = expressCodeDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging express code, criteria={}, cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.expressCode.find.fail");
        }

    }

    @Override
    public Response<ExpressCode> findById(Long id) {
        try {
            if (id == null) {
                log.error("invalid exprsssCode id is null");
                return Response.fail("expressCode.id.null");
            }

            ExpressCode expressCode = expressCodeDao.findById(id);
            return Response.ok(expressCode);
        } catch (Exception e) {
            log.error("failed to paging express code, id={}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("single.expressCode.find.fail");
        }
    }
}
