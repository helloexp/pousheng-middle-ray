package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.impl.dao.MposSkuStockDao;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by songrenfei on 2017/12/30
 */
@Component
@Slf4j
public class MposSkuStockManager {

    @Autowired
    private MposSkuStockDao mposSkuStockDao;
    /**
     * 锁定仓库库存
     * @param warehouses 待锁定的库存明细
     */
    @Transactional
    public void lockStockWarehouse(List<WarehouseShipment> warehouses) {
        for (WarehouseShipment warehouseShipment : warehouses) {
            List<SkuCodeAndQuantity> skuCodeAndQuantities = warehouseShipment.getSkuCodeAndQuantities();
            Long warehouseId = warehouseShipment.getWarehouseId();

            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Integer quantity = skuCodeAndQuantity.getQuantity();
                if(warehouseSkuStockIsExist(warehouseId,skuCode,Long.valueOf(quantity))){
                    boolean success = mposSkuStockDao.lockStockWarehouse(warehouseId,
                            skuCode,
                            quantity);
                    if (!success) {
                            log.error("lock sku stock(skuCode={}, stock={}) for warehouse(id={})",
                                    skuCode, quantity,  warehouseId);
                        throw new ServiceException("lock.sku.stock.fail");
                    }
                }
            }
        }
    }


    /**
     * 释放仓库库存
     * @param warehouses 待释放的库存明细
     */
    @Transactional
    public void unLockStockWarehouse(List<WarehouseShipment> warehouses) {
        for (WarehouseShipment warehouseShipment : warehouses) {
            List<SkuCodeAndQuantity> skuCodeAndQuantities = warehouseShipment.getSkuCodeAndQuantities();
            Long warehouseId = warehouseShipment.getWarehouseId();

            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Integer quantity = skuCodeAndQuantity.getQuantity();
                if(warehouseSkuStockIsExist(warehouseId,skuCode,Long.valueOf(quantity))){
                    boolean success = mposSkuStockDao.unlockStockWarehouse(warehouseId,
                            skuCode,
                            quantity);
                    if (!success) {
                        log.error("unlock sku stock(skuCode={}, stock={}) for warehouse(id={})",
                                skuCode, quantity,  warehouseId);
                        throw new ServiceException("unlock.sku.stock.fail");
                    }
                }
            }
        }
    }


    /**
     * 锁定门店库存
     * @param shopShipments 待锁定的库存明细
     */
    @Transactional
    public void lockStockShop(List<ShopShipment> shopShipments) {
        for (ShopShipment shopShipment : shopShipments) {
            List<SkuCodeAndQuantity> skuCodeAndQuantities = shopShipment.getSkuCodeAndQuantities();
            Long shopId = shopShipment.getShopId();

            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Integer quantity = skuCodeAndQuantity.getQuantity();
                if(checkIsNeedCreateShopSkuStock(shopId,skuCode,Long.valueOf(quantity))){
                    boolean success = mposSkuStockDao.lockStockShop(shopId,
                            skuCode,
                            quantity);
                    if (!success) {
                        log.error("lock sku stock(skuCode={}, stock={}) for shop(id={})",
                                skuCode, quantity,  shopId);
                        throw new ServiceException("lock.sku.stock.fail");
                    }
                }
            }
        }
    }


    /**
     * 释放门店库存
     * @param shopShipments 待释放的库存明细
     */
    @Transactional
    public void unLockStockShop(List<ShopShipment> shopShipments) {
        log.info("start to unlock shipment:{}",shopShipments);
        for (ShopShipment shopShipment : shopShipments) {
            List<SkuCodeAndQuantity> skuCodeAndQuantities = shopShipment.getSkuCodeAndQuantities();
            Long shopId = shopShipment.getShopId();

            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Integer quantity = skuCodeAndQuantity.getQuantity();
                if (checkIsNeedCreateShopSkuStock(shopId,skuCode,Long.valueOf(quantity))){
                    boolean success = mposSkuStockDao.unlockStockShop(shopId,
                            skuCode,
                            quantity);
                    log.info("unlock shop(id:{}) sku code:{} quantity:{}",shopId,skuCode,quantity);
                    if (!success) {
                        log.error("fail to unlock sku stock(skuCode={}, stock={}) for shop(id={})",
                                skuCode, quantity,  shopId);
                        throw new ServiceException("unlock.sku.stock.fail");
                    }
                }
            }
        }
    }



    /**
     * 锁定门店和仓库库存
     * @param shopShipments 待锁定门店的库存明细
     * @param warehouses 待锁定仓库的库存明细
     */
    @Transactional
    public void lockStockShopAndWarehouse(List<ShopShipment> shopShipments,List<WarehouseShipment> warehouses) {
        this.lockStockShop(shopShipments);
        this.lockStockWarehouse(warehouses);
    }


    private Boolean warehouseSkuStockIsExist(Long warehouseId,String skuCode,Long quantity){
        MposSkuStock exist = mposSkuStockDao.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
        if(exist ==null){
            MposSkuStock wss = new MposSkuStock();
            wss.setLockedStock(0L);
            wss.setWarehouseId(warehouseId);
            wss.setLockedStock(quantity);
            wss.setSkuCode(skuCode);
            mposSkuStockDao.create(wss);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }


    private Boolean checkIsNeedCreateShopSkuStock(Long shopId,String skuCode,Long quantity){
        MposSkuStock exist = mposSkuStockDao.findByShopIdAndSkuCode(shopId, skuCode);
        if(exist ==null){
            MposSkuStock wss = new MposSkuStock();
            wss.setLockedStock(0L);
            wss.setShopId(shopId);
            wss.setLockedStock(quantity);
            wss.setSkuCode(skuCode);
            mposSkuStockDao.create(wss);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }
}
