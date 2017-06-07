package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseSkuDao;
import com.pousheng.middle.warehouse.model.WarehouseSku;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况写服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseSkuWriteServiceImpl implements WarehouseSkuWriteService {

    private final WarehouseSkuDao warehouseSkuDao;

    @Autowired
    public WarehouseSkuWriteServiceImpl(WarehouseSkuDao warehouseSkuDao) {
        this.warehouseSkuDao = warehouseSkuDao;
    }

    @Override
    public Response<Long> create(WarehouseSku warehouseSku) {
        try {
            warehouseSkuDao.create(warehouseSku);
            return Response.ok(warehouseSku.getId());
        } catch (Exception e) {
            log.error("create warehouseSku failed, warehouseSku:{}, cause:{}", warehouseSku, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseSku warehouseSku) {
        try {
            return Response.ok(warehouseSkuDao.update(warehouseSku));
        } catch (Exception e) {
            log.error("update warehouseSku failed, warehouseSku:{}, cause:{}", warehouseSku, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseSkuId) {
        try {
            return Response.ok(warehouseSkuDao.delete(warehouseSkuId));
        } catch (Exception e) {
            log.error("delete warehouseSku failed, warehouseSkuId:{}, cause:{}", warehouseSkuId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.delete.fail");
        }
    }
}
