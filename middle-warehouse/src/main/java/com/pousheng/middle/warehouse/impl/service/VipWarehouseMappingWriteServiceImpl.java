package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.VipWarehouseMappingDao;
import com.pousheng.middle.warehouse.model.VipWarehouseMapping;
import com.pousheng.middle.warehouse.service.VipWarehouseMappingWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: zhaoxiaowei
 * Desc: 写服务实现类
 * Date: 2018-09-29
 */
@Slf4j
@Service
public class VipWarehouseMappingWriteServiceImpl implements VipWarehouseMappingWriteService {

    private final VipWarehouseMappingDao vipWarehouseMappingDao;

    @Autowired
    public VipWarehouseMappingWriteServiceImpl(VipWarehouseMappingDao vipWarehouseMappingDao) {
        this.vipWarehouseMappingDao = vipWarehouseMappingDao;
    }

    @Override
    public Response<Long> create(VipWarehouseMapping vipWarehouseMapping) {
        try {
            vipWarehouseMappingDao.create(vipWarehouseMapping);
            return Response.ok(vipWarehouseMapping.getId());
        } catch (Exception e) {
            log.error("create vipWarehouseMapping failed, vipWarehouseMapping:{}, cause:{}", vipWarehouseMapping, Throwables.getStackTraceAsString(e));
            return Response.fail("vip.warehouse.mapping.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(VipWarehouseMapping vipWarehouseMapping) {
        try {
            return Response.ok(vipWarehouseMappingDao.update(vipWarehouseMapping));
        } catch (Exception e) {
            log.error("update vipWarehouseMapping failed, vipWarehouseMapping:{}, cause:{}", vipWarehouseMapping, Throwables.getStackTraceAsString(e));
            return Response.fail("vip.warehouse.mapping.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long vipWarehouseMappingId) {
        try {
            return Response.ok(vipWarehouseMappingDao.delete(vipWarehouseMappingId));
        } catch (Exception e) {
            log.error("delete vipWarehouseMapping failed, vipWarehouseMappingId:{}, cause:{}", vipWarehouseMappingId, Throwables.getStackTraceAsString(e));
            return Response.fail("vip.warehouse.mapping.delete.fail");
        }
    }

    @Override
    public Response<Boolean> deleteByWarehouseId(Long warehouseId) {
        try {
            return Response.ok(vipWarehouseMappingDao.deleteByWarehouseId(warehouseId));
        } catch (Exception e) {
            log.error("delete vipWarehouseMapping failed, warehouseId:{}, cause:{}", warehouseId, Throwables.getStackTraceAsString(e));
            return Response.fail("vip.warehouse.mapping.delete.fail");
        }
    }
}
