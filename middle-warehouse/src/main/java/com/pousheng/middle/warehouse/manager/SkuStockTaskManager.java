package com.pousheng.middle.warehouse.manager;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.impl.dao.SkuStockTaskDao;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songrenfei on 2018/5/24
 */
@Component
@Slf4j
public class SkuStockTaskManager {


    @Autowired
    private  SkuStockTaskDao skuStockTaskDao;

    /**
     * 根据查询待处理的sku库存同步任务
     * @return sku库存同步任务
     */
    public List<SkuStockTask> findWaiteHandleLimit() {

        List<SkuStockTask> skuStockTasks = skuStockTaskDao.findWaiteHandleLimit();

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

        List list = new ArrayList();
        for (SkuStockTask skuStockTask : skuStockTasks) {
            list.add(skuStockTask.getId());
        }
        if(skuStockTaskDao.updateToHandleBatch(list)) {
            for (SkuStockTask skuStockTask : skuStockTasks) {
                validSkuStockTasks.add(skuStockTask);
            }
        }

        return validSkuStockTasks;
    }

}
