package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.MposSkuStockDao;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import com.pousheng.middle.warehouse.service.MposSkuStockWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: mpos下单sku锁定库存情况写服务实现类
 * Date: 2017-12-23
 */
@Slf4j
@Service
@RpcProvider
public class MposSkuStockWriteServiceImpl implements MposSkuStockWriteService {

    private final MposSkuStockDao mposSkuStockDao;

    @Autowired
    public MposSkuStockWriteServiceImpl(MposSkuStockDao mposSkuStockDao) {
        this.mposSkuStockDao = mposSkuStockDao;
    }

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
}
