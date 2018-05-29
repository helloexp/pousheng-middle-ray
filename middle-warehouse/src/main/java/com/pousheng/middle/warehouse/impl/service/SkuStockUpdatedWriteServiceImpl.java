package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.SkuStockTaskDao;
import com.pousheng.middle.warehouse.impl.dao.SkuStockUpdatedDao;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.model.SkuStockUpdated;
import com.pousheng.middle.warehouse.service.SkuStockTaskWriteService;
import com.pousheng.middle.warehouse.service.SkuStockUpdatedWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: lyj
 * Desc: sku库存同步任务临时表写服务实现类
 * Date: 2018-05-24
 */
@Slf4j
@Service
public class SkuStockUpdatedWriteServiceImpl implements SkuStockUpdatedWriteService {

    private final SkuStockUpdatedDao skuStockUpdatedDao;

    @Autowired
    public SkuStockUpdatedWriteServiceImpl(SkuStockUpdatedDao skuStockUpdatedDao) {
        this.skuStockUpdatedDao = skuStockUpdatedDao;
    }

    @Override
    public Response<Boolean> deleteAll() {
        try {
            return Response.ok(skuStockUpdatedDao.deleteAll());
        } catch (Exception e) {
            log.error("delete skuStockUpdate failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.updated.delete.fail");
        }
    }

    /*@Autowired
    public SkuStockUpdatedWriteServiceImpl(SkuStockUpdatedDao skuStockUpdatedDao) {
        this.skuStockUpdatedDao = skuStockUpdatedDao;
    }

    @Override
    public Response<Long> create(SkuStockUpdated skuStockUpdated) {
        try {
            skuStockUpdatedDao.create(skuStockUpdated);
            return Response.ok(skuStockUpdated.getId());
        } catch (Exception e) {
            log.error("create skuStockUpdated failed, skuStockTask:{}, cause:{}", skuStockUpdated, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.task.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(SkuStockUpdated skuStockUpdated) {
        try {
            return Response.ok(skuStockUpdatedDao.update(skuStockUpdated));
        } catch (Exception e) {
            log.error("update skuStockTask failed, skuStockUpdated:{}, cause:{}", skuStockUpdated, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.task.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long skuStockTaskId) {
        try {
            return Response.ok(skuStockUpdatedDao.delete(skuStockTaskId));
        } catch (Exception e) {
            log.error("delete skuStockTask failed, skuStockTaskId:{}, cause:{}", skuStockTaskId, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.task.delete.fail");
        }
    }

    @Override
    public Response<Boolean> updateTimeOutHandleTask() {

        try {
            return Response.ok(skuStockUpdatedDao.updateTimeOutHandleTask());
        } catch (Exception e) {
            log.error("updateTimeOutHandleTask failed cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("update.sku.stock.task.fail");
        }
    }

    @Override
    public Response<Boolean> updateStatusById(Long skuStockUpdatedId,int status) {
        try {
            return Response.ok(skuStockUpdatedDao.updateStatusById(skuStockUpdatedId,status));
        } catch (Exception e) {
            log.error("update skuStockTask status failed, skuStockUpdatedId:{}, cause:{}", skuStockUpdatedId, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.task.update.status.fail");
        }
    }*/

}
