package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.impl.dao.WarehouseSkuStockDao;
import com.pousheng.middle.warehouse.manager.WarehouseSkuStockManager;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况写服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseSkuWriteServiceImpl implements WarehouseSkuWriteService {

    private final WarehouseSkuStockDao warehouseSkuStockDao;

    private final WarehouseSkuStockManager warehouseSkuStockManager;

    @Autowired
    public WarehouseSkuWriteServiceImpl(WarehouseSkuStockDao warehouseSkuStockDao,
                                        WarehouseSkuStockManager warehouseSkuStockManager) {
        this.warehouseSkuStockDao = warehouseSkuStockDao;
        this.warehouseSkuStockManager = warehouseSkuStockManager;
    }

    @Override
    public Response<Long> create(WarehouseSkuStock warehouseSkuStock) {
        try {
            warehouseSkuStockDao.create(warehouseSkuStock);
            return Response.ok(warehouseSkuStock.getId());
        } catch (Exception e) {
            log.error("batchCreate warehouseSkuStock failed, warehouseSkuStock:{}, cause:{}", warehouseSkuStock, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.batchCreate.fail");
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

    /**
     * 根据指定的仓库分配策略锁定库存
     *
     * @param warehouseShipments 仓库及发货数量列表
     * @return 是否扣减成功
     */
    @Override
    public Response<Boolean> lockStock(List<WarehouseShipment> warehouseShipments) {
        try {
            warehouseSkuStockManager.lockStock(warehouseShipments);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to lock stock for {}", warehouseShipments, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.stock.lock.fail");
        }
    }

    /**
     * 根据实际出库的库存情况来变更库存, 这里需要先恢复原来锁定的仓库明细, 然后再根据实际库存做扣减
     *
     * @param lockedShipments 之前锁定的仓库明细
     * @param actualShipments 实际仓库发货明细
     * @return 是否变更成功
     */
    @Override
    public Response<Boolean> decreaseStock(List<WarehouseShipment> lockedShipments,
                                           List<WarehouseShipment> actualShipments) {
        try {
            warehouseSkuStockManager.decreaseStock(lockedShipments,actualShipments);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to decrease stock for {}", actualShipments, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.stock.decrease.fail");
        }
    }
}
