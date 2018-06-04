package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.MposSkuStockDao;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import com.pousheng.middle.warehouse.service.MposSkuStockReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: mpos下单sku锁定库存情况读服务实现类
 * Date: 2017-12-23
 */
@Slf4j
@Service
@RpcProvider
public class MposSkuStockReadServiceImpl implements MposSkuStockReadService {

    private final MposSkuStockDao mposSkuStockDao;

    @Autowired
    public MposSkuStockReadServiceImpl(MposSkuStockDao mposSkuStockDao) {
        this.mposSkuStockDao = mposSkuStockDao;
    }

    @Override
    public Response<MposSkuStock> findById(Long Id) {
        try {
            return Response.ok(mposSkuStockDao.findById(Id));
        } catch (Exception e) {
            log.error("find mposSkuStock by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("mpos.sku.stock.find.fail");
        }
    }

    @Override
    public Response<Optional<MposSkuStock>> findByWarehouseIdAndSkuCode(Long warehouseId, String skuCode) {
        try {
            return Response.ok(Optional.fromNullable(mposSkuStockDao.findByWarehouseIdAndSkuCode(warehouseId,skuCode)));
        } catch (Exception e) {
            log.error("find mposSkuStock by warehouse id :{} and sku code:{} failed,  cause:{}", warehouseId,skuCode, Throwables.getStackTraceAsString(e));
            return Response.fail("mpos.sku.stock.find.fail");
        }
    }

    @Override
    public Response<Optional<MposSkuStock>> findByShopIdAndSkuCode(Long shopId, String skuCode) {
        try {
            return Response.ok(Optional.fromNullable(mposSkuStockDao.findByShopIdAndSkuCode(shopId,skuCode)));
        } catch (Exception e) {
            log.error("find mposSkuStock by shop id :{} and sku code:{} failed,  cause:{}", shopId,skuCode, Throwables.getStackTraceAsString(e));
            return Response.fail("mpos.sku.stock.find.fail");
        }
    }
}
