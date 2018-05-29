package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.SkuStockTaskDao;
import com.pousheng.middle.warehouse.impl.dao.SkuStockUpdatedDao;
import com.pousheng.middle.warehouse.manager.SkuStockTaskManager;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.model.SkuStockUpdated;
import com.pousheng.middle.warehouse.service.SkuStockUpdatedReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: lyj
 * Desc: sku库存同步任务临时表读服务实现类
 * Date: 2018-05-29
 */
@Slf4j
@Service
public class SkuStockUpdatedReadServiceImpl implements SkuStockUpdatedReadService {

    private final SkuStockUpdatedDao skuStockUpdatedDao;

    @Autowired
    public SkuStockUpdatedReadServiceImpl(SkuStockUpdatedDao skuStockUpdatedDao) {
        this.skuStockUpdatedDao = skuStockUpdatedDao;
    }

    @Override
    public Response<List<String>> findWaiteHandle() {
        try {
            return Response.ok(skuStockUpdatedDao.findWaiteHandle());
        } catch (Exception e) {
            log.error("findWaiteHandleLimit failed,cause:{}",Throwables.getStackTraceAsString(e));
            return Response.fail("find.sku.stock.task");
        }
    }
}
