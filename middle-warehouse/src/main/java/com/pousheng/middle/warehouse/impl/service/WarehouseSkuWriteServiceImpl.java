package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseSkuStockDao;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
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

    private final WarehouseSkuStockDao warehouseSkuStockDao;

    @Autowired
    public WarehouseSkuWriteServiceImpl(WarehouseSkuStockDao warehouseSkuStockDao) {
        this.warehouseSkuStockDao = warehouseSkuStockDao;
    }

    @Override
    public Response<Long> create(WarehouseSkuStock warehouseSkuStock) {
        try {
            warehouseSkuStockDao.create(warehouseSkuStock);
            return Response.ok(warehouseSkuStock.getId());
        } catch (Exception e) {
            log.error("create warehouseSkuStock failed, warehouseSkuStock:{}, cause:{}", warehouseSkuStock, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseSkuStock warehouseSkuStock) {
        try {
            return Response.ok(warehouseSkuStockDao.update(warehouseSkuStock));
        } catch (Exception e) {
            log.error("update warehouseSkuStock failed, warehouseSkuStock:{}, cause:{}", warehouseSkuStock, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseSkuId) {
        try {
            return Response.ok(warehouseSkuStockDao.delete(warehouseSkuId));
        } catch (Exception e) {
            log.error("delete warehouseSku failed, warehouseSkuId:{}, cause:{}", warehouseSkuId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.delete.fail");
        }
    }
}
