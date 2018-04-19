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
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Created by tony on 2017/6/28
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

    @Override
    public Response<List<ExpressCode>> findAllByName(String name) {
        try{
           return Response.ok(expressCodeDao.findAllByName(name));
        }catch (Exception e){
            log.error("failed to find all express code, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("all.expressCode.find.fail");
        }
    }


    /**
     * 按照快递名称查询快递
     */
    @Override
    public Response<ExpressCode> findByName(String name) {
        try {
            if (!StringUtils.hasText(name)) {
                return Response.fail("express.name.not.null");
            }
            return Response.ok(expressCodeDao.findByName(name));
        } catch (Exception e) {
            log.error("failed to find  express code, name:{}, cause:{}",name, Throwables.getStackTraceAsString(e));
            return Response.fail("find.expressCode.by.name.fail");
        }

    }
}
