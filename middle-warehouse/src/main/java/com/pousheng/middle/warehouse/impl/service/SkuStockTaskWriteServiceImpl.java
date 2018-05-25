package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.SkuStockTaskDao;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.service.SkuStockTaskWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: sku库存同步任务写服务实现类
 * Date: 2018-05-24
 */
@Slf4j
@Service
public class SkuStockTaskWriteServiceImpl implements SkuStockTaskWriteService {

    private final SkuStockTaskDao skuStockTaskDao;

    @Autowired
    public SkuStockTaskWriteServiceImpl(SkuStockTaskDao skuStockTaskDao) {
        this.skuStockTaskDao = skuStockTaskDao;
    }

    @Override
    public Response<Long> create(SkuStockTask skuStockTask) {
        try {
            skuStockTaskDao.create(skuStockTask);
            return Response.ok(skuStockTask.getId());
        } catch (Exception e) {
            log.error("create skuStockTask failed, skuStockTask:{}, cause:{}", skuStockTask, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.task.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(SkuStockTask skuStockTask) {
        try {
            return Response.ok(skuStockTaskDao.update(skuStockTask));
        } catch (Exception e) {
            log.error("update skuStockTask failed, skuStockTask:{}, cause:{}", skuStockTask, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.task.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long skuStockTaskId) {
        try {
            return Response.ok(skuStockTaskDao.delete(skuStockTaskId));
        } catch (Exception e) {
            log.error("delete skuStockTask failed, skuStockTaskId:{}, cause:{}", skuStockTaskId, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.task.delete.fail");
        }
    }

    @Override
    public Response<Boolean> updateTimeOutHandleTask() {

        try {
            return Response.ok(skuStockTaskDao.updateTimeOutHandleTask());
        } catch (Exception e) {
            log.error("updateTimeOutHandleTask failed cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("update.sku.stock.task.fail");
        }
    }
}
