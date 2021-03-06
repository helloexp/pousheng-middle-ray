package com.pousheng.middle.order.impl.service;

import com.google.common.base.Objects;
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
    private final ExpressCodeDao expressCodeDao;

    @Autowired
    public ExpressCodeWriteServiceImpl(ExpressCodeDao expressCodeDao) {
        this.expressCodeDao = expressCodeDao;
    }

    @Override
    public Response<Long> create(ExpressCode expressCode) {
        try {
            if (StringUtils.hasText(expressCode.getName())) {
                ExpressCode exist = expressCodeDao.findByName(expressCode.getName());
                if (exist != null) {
                    log.error("duplicated name({}) with existed(name={})", expressCode.getName(), exist.getName());
                    return Response.fail("expressCode.name.duplicate");
                }
                ExpressCode exist1 = expressCodeDao.findByOfficalCode(expressCode.getOfficalCode());
                if (exist1!=null){
                    log.error("duplicated offical code ({}) with existed(officalCode={})", expressCode.getOfficalCode(), exist1.getOfficalCode());
                    return Response.fail("expressCode.officalCode.duplicate");
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
            if (StringUtils.hasText(expressCode.getName())) {
                //判断快递名称是否能够改成其他的
                ExpressCode exist = expressCodeDao.findByName(expressCode.getName());
                if (exist != null&& !Objects.equal(expressCode.getId(),exist.getId())) {
                    log.error("duplicated name({}) with existed(name={})", expressCode.getName(), exist.getName());
                    return Response.fail("expressCode.name.duplicate");
                }
            }
            //判断官方代码要保持唯一性
            if (StringUtils.hasText(expressCode.getOfficalCode())){
                ExpressCode exist = expressCodeDao.findByOfficalCode(expressCode.getOfficalCode());
                if (exist!=null&& !Objects.equal(expressCode.getId(),exist.getId())){
                    log.error("duplicated offical code ({}) with existed(officalCode={})", expressCode.getOfficalCode(), exist.getOfficalCode());
                    return Response.fail("expressCode.officalCode.duplicate");
                }
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
