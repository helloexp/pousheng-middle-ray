package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.impl.dao.SkuStockTaskDao;
import com.pousheng.middle.warehouse.manager.SkuStockTaskManager;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.service.SkuStockTaskReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: sku库存同步任务读服务实现类
 * Date: 2018-05-24
 */
@Slf4j
@Service
public class SkuStockTaskReadServiceImpl implements SkuStockTaskReadService {

    private final SkuStockTaskDao skuStockTaskDao;
    private final SkuStockTaskManager skuStockTaskManager;

    @Autowired
    public SkuStockTaskReadServiceImpl(SkuStockTaskDao skuStockTaskDao, SkuStockTaskManager skuStockTaskManager) {
        this.skuStockTaskDao = skuStockTaskDao;
        this.skuStockTaskManager = skuStockTaskManager;
    }

    @Override
    public Response<SkuStockTask> findById(Long id) {
        try {
            return Response.ok(skuStockTaskDao.findById(id));
        } catch (Exception e) {
            log.error("find skuStockTask by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.stock.task.find.fail");
        }
    }

    @Override
    public Response<List<SkuStockTask>> findWaiteHandleLimit() {
        try {

            return Response.ok(skuStockTaskManager.findWaiteHandleLimit());

        } catch (Exception e) {
            log.error("findWaiteHandleLimit failed,cause:{}",Throwables.getStackTraceAsString(e));
            return Response.fail("find.sku.stock.task");
        }

    }
}
