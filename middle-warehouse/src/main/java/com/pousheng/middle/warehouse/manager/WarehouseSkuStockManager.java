package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.impl.dao.WarehouseSkuStockDao;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@Component
@Slf4j
public class WarehouseSkuStockManager {

    private final WarehouseSkuStockDao warehouseSkuStockDao;

    @Autowired
    public WarehouseSkuStockManager(WarehouseSkuStockDao warehouseSkuStockDao) {
        this.warehouseSkuStockDao = warehouseSkuStockDao;
    }

    /**
     * 锁定库存
     * @param warehouses 待锁定的库存明细
     */
    @Transactional
    public void lockStock(List<WarehouseShipment> warehouses) {
        for (WarehouseShipment warehouseShipment : warehouses) {
            List<SkuCodeAndQuantity> skuCodeAndQuantities = warehouseShipment.getSkuCodeAndQuantities();
            Long warehouseId = warehouseShipment.getWarehouseId();

            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Integer quantity = skuCodeAndQuantity.getQuantity();
                boolean success = warehouseSkuStockDao.lockStock(warehouseId,
                        skuCode,
                        quantity);
                if (!success) {
                    WarehouseSkuStock wss = this.warehouseSkuStockDao.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
                    if (wss == null) {
                        log.error("no sku(skuCode={}) stock found in warehouse(id={})", skuCode, warehouseId);
                    } else {
                        log.error("insufficient sku stock(skuCode={}, required stock={}, actual stock={}) for warehouse(id={})",
                                skuCode, quantity, wss.getAvailStock(), warehouseId);

                    }
                    throw new ServiceException("insufficient.sku.stock");
                }
            }
        }
    }

    /**
     * 先解锁之前的锁定的库存, 在扣减实际发货的库存
     *
     * @param lockedShipments 之前锁定的库存明细
     * @param actualShipments 实际发货的库存明细
     */
    @Transactional
    public void decreaseStock(List<WarehouseShipment> lockedShipments, List<WarehouseShipment> actualShipments) {
        doUnlock(lockedShipments);
        for (WarehouseShipment actualShipment : actualShipments) {
            List<SkuCodeAndQuantity> skuCodeAndQuantities = actualShipment.getSkuCodeAndQuantities();
            Long warehouseId = actualShipment.getWarehouseId();
            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Integer quantity = skuCodeAndQuantity.getQuantity();
                boolean success = warehouseSkuStockDao.decreaseStock(warehouseId,
                        skuCode,
                        quantity);
                if(!success){
                    log.error("failed to decrease stock of warehouse where warehouseId={} and skuCode={}, delta={}",
                            warehouseId, skuCode, quantity );
                    throw new ServiceException("stock.decrease.fail");
                }
            }
        }

    }

    /**
     * 根据指定的仓库分配策略解锁库存, 当撤销发货单时, 调用这个接口
     *
     * @param warehouseShipments 仓库及解锁数量列表
     */
    @Transactional
    public void unlockStock(List<WarehouseShipment> warehouseShipments) {
        doUnlock(warehouseShipments);
    }

    private void doUnlock(List<WarehouseShipment> lockedShipments) {
        for (WarehouseShipment lockedShipment : lockedShipments) {
            List<SkuCodeAndQuantity> skuCodeAndQuantities = lockedShipment.getSkuCodeAndQuantities();
            Long warehouseId = lockedShipment.getWarehouseId();
            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Integer quantity = skuCodeAndQuantity.getQuantity();
                boolean success = warehouseSkuStockDao.unlockStock(warehouseId,
                        skuCode,
                        quantity);
                if(!success){
                    log.error("failed to unlock stock of warehouse where warehouseId={} and skuCode={}, delta={}",
                            warehouseId, skuCode, quantity );
                    throw new ServiceException("stock.unlock.fail");
                }
            }
        }
    }

}
