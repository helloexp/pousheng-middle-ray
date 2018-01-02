package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.impl.dao.MposSkuStockDao;
import com.pousheng.middle.warehouse.manager.MposSkuStockManager;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import com.pousheng.middle.warehouse.service.MposSkuStockWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: mpos下单sku锁定库存情况写服务实现类
 * Date: 2017-12-23
 */
@Slf4j
@Service
@RpcProvider
public class MposSkuStockWriteServiceImpl implements MposSkuStockWriteService {

    @Autowired
    private MposSkuStockDao mposSkuStockDao;
    @Autowired
    private MposSkuStockManager mposSkuStockManager;


    @Override
    public Response<Long> create(MposSkuStock mposSkuStock) {
        try {
            mposSkuStockDao.create(mposSkuStock);
            return Response.ok(mposSkuStock.getId());
        } catch (Exception e) {
            log.error("create mposSkuStock failed, mposSkuStock:{}, cause:{}", mposSkuStock, Throwables.getStackTraceAsString(e));
            return Response.fail("mpos.sku.stock.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(MposSkuStock mposSkuStock) {
        try {
            return Response.ok(mposSkuStockDao.update(mposSkuStock));
        } catch (Exception e) {
            log.error("update mposSkuStock failed, mposSkuStock:{}, cause:{}", mposSkuStock, Throwables.getStackTraceAsString(e));
            return Response.fail("mpos.sku.stock.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long mposSkuStockId) {
        try {
            return Response.ok(mposSkuStockDao.delete(mposSkuStockId));
        } catch (Exception e) {
            log.error("delete mposSkuStock failed, mposSkuStockId:{}, cause:{}", mposSkuStockId, Throwables.getStackTraceAsString(e));
            return Response.fail("mpos.sku.stock.delete.fail");
        }
    }

    @Override
    public Response<Boolean> lockStockWarehouse(List<WarehouseShipment> warehouseShipments) {
        try {
            mposSkuStockManager.lockStockWarehouse(warehouseShipments);
            return Response.ok();
        }catch (Exception e){
            log.error("mpos failed to lock warehouse stock for {}", warehouseShipments, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.stock.lock.fail");
        }
    }

    @Override
    public Response<Boolean> unlockStockWarehouse(List<WarehouseShipment> warehouseShipments) {
        try {
            mposSkuStockManager.unLockStockWarehouse(warehouseShipments);
            return Response.ok();
        }catch (Exception e){
            log.error("mpos failed to unlock warehouse stock for {}", warehouseShipments, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.stock.unlock.fail");
        }    }

    @Override
    public Response<Boolean> lockStockShop(List<ShopShipment> shopShipments) {
        try {
            mposSkuStockManager.lockStockShop(shopShipments);
            return Response.ok();
        }catch (Exception e){
            log.error("mpos failed to lock shop stock for {}", shopShipments, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.stock.lock.fail");
        }
    }

    @Override
    public Response<Boolean> unLockStockShop(List<ShopShipment> shopShipments) {
        try {
            mposSkuStockManager.unLockStockShop(shopShipments);
            return Response.ok();
        }catch (Exception e){
            log.error("mpos failed to unlock shop stock for {}", shopShipments, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.stock.unlock.fail");
        }
    }

    @Override
    public Response<Boolean> lockStockShopAndWarehouse(List<ShopShipment> shopShipments, List<WarehouseShipment> warehouseShipments) {
        try {
            mposSkuStockManager.lockStockShopAndWarehouse(shopShipments,warehouseShipments);
            return Response.ok();
        }catch (Exception e){
            log.error("mpos failed to lock shop stock shopShipments: {},warehouseShipments: {}", shopShipments,warehouseShipments, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.stock.lock.fail");
        }
    }
}
