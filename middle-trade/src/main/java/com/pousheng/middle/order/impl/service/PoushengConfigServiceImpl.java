package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.PoushengConfigDao;
import com.pousheng.middle.order.model.PoushengConfig;
import com.pousheng.middle.order.service.PoushengConfigService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/28
 */
@Slf4j
@Service
public class PoushengConfigServiceImpl implements PoushengConfigService {

    private final PoushengConfigDao poushengConfigDao;

    @Autowired
    public PoushengConfigServiceImpl(PoushengConfigDao poushengConfigDao) {
        this.poushengConfigDao = poushengConfigDao;
    }

    @Override
    public Response<PoushengConfig> findPoushengConfigById(Long poushengConfigId) {
        try {
            return Response.ok(poushengConfigDao.findById(poushengConfigId));
        } catch (Exception e) {
            log.error("find poushengConfig by id failed, poushengConfigId:{}, cause:{}", poushengConfigId, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengConfig.find.fail");
        }
    }

    @Override
    public Response<PoushengConfig> findPoushengConfigByType(String type) {
        try {
            return Response.ok(poushengConfigDao.findByType(type));
        } catch (Exception e) {
            log.error("find poushengConfig by type failed, type:{}, cause:{}", type, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengConfig.find.fail");
        }
    }

    @Override
    public Response<Long> createPoushengConfig(PoushengConfig poushengConfig) {
        try {
            poushengConfigDao.create(poushengConfig);
            return Response.ok(poushengConfig.getId());
        } catch (Exception e) {
            log.error("create poushengConfig failed, poushengConfig:{}, cause:{}", poushengConfig, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengConfig.create.fail");
        }
    }

    @Override
    public Response<Boolean> updatePoushengConfig(PoushengConfig poushengConfig) {
        try {
            return Response.ok(poushengConfigDao.update(poushengConfig));
        } catch (Exception e) {
            log.error("update poushengConfig failed, poushengConfig:{}, cause:{}", poushengConfig, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengConfig.update.fail");
        }
    }

    @Override
    public Response<Boolean> deletePoushengConfigById(Long poushengConfigId) {
        try {
            return Response.ok(poushengConfigDao.delete(poushengConfigId));
        } catch (Exception e) {
            log.error("delete poushengConfig failed, poushengConfigId:{}, cause:{}", poushengConfigId, Throwables.getStackTraceAsString(e));
            return Response.fail("poushengConfig.delete.fail");
        }
    }
}
