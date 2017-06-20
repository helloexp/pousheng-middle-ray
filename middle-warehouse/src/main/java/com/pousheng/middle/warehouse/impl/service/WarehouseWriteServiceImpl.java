package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseDao;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Author: jlchen
 * Desc: 仓库写服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseWriteServiceImpl implements WarehouseWriteService {

    private final WarehouseDao warehouseDao;

    @Autowired
    public WarehouseWriteServiceImpl(WarehouseDao warehouseDao) {
        this.warehouseDao = warehouseDao;
    }

    @Override
    public Response<Long> create(Warehouse warehouse) {
        try {
            if(StringUtils.hasText(warehouse.getCode())){
                Warehouse exist = warehouseDao.findByCode(warehouse.getCode());
                if(exist !=null){
                    log.error("duplicated warehouse code({}) with existed(id={})",warehouse.getCode(), exist.getId());
                    return Response.fail("warehouse.code.duplicate");
                }
            }
            warehouseDao.create(warehouse);
            return Response.ok(warehouse.getId());
        } catch (Exception e) {
            log.error("batchCreate warehouse failed, warehouse:{}, cause:{}", warehouse, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.batchCreate.fail");
        }
    }

    @Override
    public Response<Boolean> update(Warehouse warehouse) {
        try {
            if(StringUtils.hasText(warehouse.getCode())){
                Warehouse exist = warehouseDao.findByCode(warehouse.getCode());
                if(exist !=null){
                    log.error("duplicated warehouse code({}) with existed(id={})",warehouse.getCode(), exist.getId());
                    return Response.fail("warehouse.code.duplicate");
                }
            }
            return Response.ok(warehouseDao.update(warehouse));
        } catch (Exception e) {
            log.error("update warehouse failed, warehouse:{}, cause:{}", warehouse, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseId) {
        try {
            return Response.ok(warehouseDao.delete(warehouseId));
        } catch (Exception e) {
            log.error("delete warehouse failed, warehouseId:{}, cause:{}", warehouseId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.delete.fail");
        }
    }
}
