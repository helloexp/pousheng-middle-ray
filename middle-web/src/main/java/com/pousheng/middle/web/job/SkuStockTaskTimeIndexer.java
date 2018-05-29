package com.pousheng.middle.web.job;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.service.SkuStockTaskReadService;
import com.pousheng.middle.warehouse.service.SkuStockTaskWriteService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SkuStockTaskTimeIndexer {

    @Autowired
    private SkuStockTaskReadService skuStockTaskReadService;

    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;

    @RpcConsumer
    private SkuStockTaskWriteService skuStockTaskWriteService;
    @Autowired
    private SkuStockExecutor skuStockExecutor;

    @PostConstruct
    public void doIndex() {
        new Thread(new IndexTask(skuStockTaskReadService, warehouseSkuWriteService, skuStockTaskWriteService)).start();
    }


    class IndexTask implements Runnable {

        private final SkuStockTaskReadService skuStockTaskReadService;
        private final WarehouseSkuWriteService warehouseSkuWriteService;
        private final SkuStockTaskWriteService skuStockTaskWriteService;

        public IndexTask(SkuStockTaskReadService skuStockTaskReadService,
                         WarehouseSkuWriteService warehouseSkuWriteService,
                         SkuStockTaskWriteService skuStockTaskWriteService) {
            this.skuStockTaskReadService = skuStockTaskReadService;
            this.warehouseSkuWriteService = warehouseSkuWriteService;
            this.skuStockTaskWriteService = skuStockTaskWriteService;
        }



        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);


            try {
                while (true){
                    int capacity = skuStockExecutor.remainingCapacity();
                    log.info("skuStockExecutor middle queue capacity:{}",capacity);

                    if (capacity > 0) {
                        // fetch data
                        Response<List<SkuStockTask>> listRes = skuStockTaskReadService.findWaiteHandleLimit(capacity,0);
                            if (listRes.isSuccess() && !CollectionUtils.isEmpty(listRes.getResult())) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Fetch data {}",
                                            listRes.getResult().stream()
                                                    .map(SkuStockTask::getId).collect(Collectors.toList()));
                                }

                            listRes.getResult().forEach(
                                    skuStockTask -> skuStockExecutor.submit(new SotckPushTask(skuStockTask))
                            );
                        }
                    }
                    Thread.sleep(30000);

                }
            } catch (Exception e) {
                log.warn("fail to process sku stock, cause:{}", Throwables.getStackTraceAsString(e));
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
            handleSyncStock(skuStockTask);
        }
    }


    private void handleSyncStock(SkuStockTask skuStockTask) {

        log.debug("start handle sku stock to middle db task (id:{}) to middle", skuStockTask.getId());

        List<StockDto> stockDtos = skuStockTask.getStockDtoList();

        try {
            Response<Boolean> r = warehouseSkuWriteService.syncStock(stockDtos);
            if (!r.isSuccess()) {
                log.error("failed to handle stock task(id:{}) to middle db, data:{},error:{}", skuStockTask.getId(), stockDtos, r.getError());
                return;
            }
            log.debug("start update sku stock task (id:{}) to middle db status to 2", skuStockTask.getId());
            Response<Boolean> updateRes = skuStockTaskWriteService.updateStatusById(skuStockTask.getId(),2);
            if (!updateRes.isSuccess()) {
                log.error("update sku stock task by id:{} to middle db  over fail,error:{}", skuStockTask.getId(), updateRes.getError());
            }
            log.debug("end handle sku stock task (id:{}) to middle db", skuStockTask.getId());
        } catch (Exception e) {
            log.error("failed to update stocks task(id:{}) to middle db, data:{},cause:{}", skuStockTask.getId(), stockDtos, Throwables.getStackTraceAsString(e));
        }

    }
}
