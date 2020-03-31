package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.VipWarehouseMappingDao;
import com.pousheng.middle.warehouse.model.VipWarehouseMapping;
import com.pousheng.middle.warehouse.service.VipWarehouseMappingReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: zhaoxiaowei
 * Desc: 读服务实现类
 * Date: 2018-09-29
 */
@Slf4j
@Service
public class VipWarehouseMappingReadServiceImpl implements VipWarehouseMappingReadService {

    @Autowired
    private VipWarehouseMappingDao vipWarehouseMappingDao;

    @Override
    public Response<VipWarehouseMapping> findById(Long Id) {
        try {
            return Response.ok(vipWarehouseMappingDao.findById(Id));
        } catch (Exception e) {
            log.error("find vipWarehouseMapping by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("vip.warehouse.mapping.find.fail");
        }
    }


    @Override
    public Response<VipWarehouseMapping> findByWarehouseId(Long warehouseId) {
        try {
            return Response.ok(vipWarehouseMappingDao.findByWarehouseId(warehouseId));
        } catch (Exception e) {
            log.error("find vipWarehouseMapping by warehouseId :{} failed,  cause:{}", warehouseId, Throwables.getStackTraceAsString(e));
            return Response.fail("vip.warehouse.mapping.find.fail");
        }
    }


    @Override
    public Response<List<VipWarehouseMapping>> findAll() {
        try {
            return Response.ok(vipWarehouseMappingDao.findAll());
        } catch (Exception e) {
            log.error("find vipWarehouseMapping list failed,  cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("vip.warehouse.mapping.find.fail");
        }
    }
}
