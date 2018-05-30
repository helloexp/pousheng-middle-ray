package com.pousheng.middle.warehouse.manager;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.impl.dao.SkuStockTaskDao;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * Created by songrenfei on 2018/5/24
 */
@Component
@Slf4j
public class SkuStockTaskManager {


    @Autowired
    private SkuStockTaskDao skuStockTaskDao;

    /**
     * 根据查询待处理的sku库存同步任务
     *
     * @return sku库存同步任务
     */
    public List<SkuStockTask> findWaiteHandleLimit(int qty,Integer status,String type) {

        List<SkuStockTask> skuStockTasks = skuStockTaskDao.findWaiteHandleLimit(qty,status,type);

        List<SkuStockTask> validSkuStockTasks = Lists.newArrayListWithCapacity(skuStockTasks.size());
        if (CollectionUtils.isEmpty(skuStockTasks)) {
            return validSkuStockTasks;
        }

        /*
        for (SkuStockTask skuStockTask : skuStockTasks) {
            if (skuStockTaskDao.updateToHandle(skuStockTask.getId(), DateTime.now().plusMinutes(10).toDate())) {
                validSkuStockTasks.add(skuStockTask);
            }
        }*/

        List<Long> ids = Lists.newArrayList();
        for (SkuStockTask skuStockTask : skuStockTasks) {
            ids.add(skuStockTask.getId());
        }

        if (Objects.equals(status, 0)) {
            if (skuStockTaskDao.updateToHandleBatch(ids, 0, 1)) {
                validSkuStockTasks.addAll(skuStockTasks);
            }
        }

        if (Objects.equals(status, 2)) {
            if (skuStockTaskDao.updateToHandleBatch(ids, 2, 3)) {
                validSkuStockTasks.addAll(skuStockTasks);
            }
        }
        return validSkuStockTasks;
    }

}
