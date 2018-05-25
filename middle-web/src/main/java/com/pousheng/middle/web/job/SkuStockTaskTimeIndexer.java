package com.pousheng.middle.web.job;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.service.SkuStockTaskReadService;
import com.pousheng.middle.warehouse.service.SkuStockTaskWriteService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
public class SkuStockTaskTimeIndexer {

    @Autowired
    private SkuStockTaskReadService skuStockTaskReadService;

    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;

    @RpcConsumer
    private SkuStockTaskWriteService skuStockTaskWriteService;
    @Autowired
    private StockPusher stockPusher;
    @Autowired
    private SkuStockExecutor skuStockExecutor;

    @PostConstruct
    public void doIndex() {
        new Thread(new IndexTask(skuStockTaskReadService, warehouseSkuWriteService, stockPusher, skuStockTaskWriteService)).start();
    }


    class IndexTask implements Runnable {

        private final SkuStockTaskReadService skuStockTaskReadService;
        private final WarehouseSkuWriteService warehouseSkuWriteService;
        private final StockPusher stockPusher;
        private final SkuStockTaskWriteService skuStockTaskWriteService;

        public IndexTask(SkuStockTaskReadService skuStockTaskReadService,
                         WarehouseSkuWriteService warehouseSkuWriteService, StockPusher stockPusher,
                         SkuStockTaskWriteService skuStockTaskWriteService) {
            this.skuStockTaskReadService = skuStockTaskReadService;
            this.warehouseSkuWriteService = warehouseSkuWriteService;
            this.stockPusher = stockPusher;
            this.skuStockTaskWriteService = skuStockTaskWriteService;
        }


        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p/>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            log.info("start handle sku stock task");
            while (true) {
                try {
                    Response<List<SkuStockTask>> listRes = skuStockTaskReadService.findWaiteHandleLimit();
                    if (!listRes.isSuccess()) {
                        log.error("findWaiteHandleLimit fail,error:{}", listRes.getError());
                        Thread.sleep(100000);
                    }

                    List<SkuStockTask> skuStockTasks = listRes.getResult();
                    if (!CollectionUtils.isEmpty(skuStockTasks)) {
                        for (SkuStockTask skuStockTask : skuStockTasks) {
                            //log.info("PUT STOCK TASK:{} to thread pool",skuStockTask.getId());
                            skuStockExecutor.submit(new SotckPushTask(skuStockTask));
                        }
                    } else {
                        Thread.sleep(100000);
                    }
                } catch (Exception e) {
                    log.warn("fail to process sku stock, cause:{}", e.getMessage());
                }
            }
        }
    }


    private class SotckPushTask implements Runnable {

        private final SkuStockTask skuStockTask;

        private SotckPushTask(SkuStockTask skuStockTask) {
            this.skuStockTask = skuStockTask;
        }

        @Override
        public void run() {
            log.info("STOCK PUSH THREAD ID:{}",Thread.currentThread().getId());
            handleSyncStock(skuStockTask);
        }
    }


    private void handleSyncStock(SkuStockTask skuStockTask) {

        log.debug("start handle sku stock task (id:{}) to middle", skuStockTask.getId());

        List<StockDto> stockDtos = skuStockTask.getStockDtoList();

        try {
            Response<Boolean> r = warehouseSkuWriteService.syncStock(stockDtos);
            if (!r.isSuccess()) {
                log.error("failed to handle stock task(id:{}), data:{},error:{}", skuStockTask.getId(), stockDtos, r.getError());
            }
            //触发库存推送
            List<String> skuCodes = Lists.newArrayList();
            for (StockDto stockDto : stockDtos) {
                skuCodes.add(stockDto.getSkuCode());
            }
            stockPusher.submit(skuCodes);

            log.debug("end handle sku stock task (id:{}) to middle", skuStockTask.getId());
        } catch (Exception e) {
            log.error("failed to sync stocks task(id:{}), data:{},cause:{}", skuStockTask.getId(), stockDtos, Throwables.getStackTraceAsString(e));
        }

        log.debug("start delete sku stock task (id:{})", skuStockTask.getId());
        Response<Boolean> deleteRes = skuStockTaskWriteService.deleteById(skuStockTask.getId());
        if (!deleteRes.isSuccess()) {
            log.error("delete sku stock task by id:{} fail,error:{}", skuStockTask.getId(), deleteRes.getError());
        }

    }
}
