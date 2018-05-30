package com.pousheng.middle.web.job;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.enums.StockTaskType;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.service.SkuStockTaskReadService;
import com.pousheng.middle.warehouse.service.SkuStockTaskWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SkuStockThirdTaskTimeIndexer {

    @Autowired
    private SkuStockTaskReadService skuStockTaskReadService;

    @RpcConsumer
    private SkuStockTaskWriteService skuStockTaskWriteService;
    @Autowired
    private StockPusher stockPusher;
    @Autowired
    private SkuStockThirdExecutor skuStockThridExecutor;

    @Value("${stock.sync.third.full.time.start:00:00:00}")
    String stockSyncThirdFullTimeStart;
    @Value("${stock.sync.third.full.time.end:00:30:00}")
    String stockSyncThirdFullTimeEnd;

    @PostConstruct
    public void doIndex() {
        new Thread(new IndexTask()).start();
    }

    class IndexTask implements Runnable {

        @Override
        public void run() {

            try {

                while (true){
                    int capacity = skuStockThridExecutor.remainingCapacity();
                    log.info("skuStockExecutor third queue capacity:{}",capacity);

                    if (capacity > 0) {
                        // fetch data
                        if(!isFullRunTime()) {
                           String type = StockTaskType.INCR_TYPE.getValue();
                            Response<List<SkuStockTask>> listRes = skuStockTaskReadService.findWaiteHandleLimit(capacity, 2, type);
                            if (listRes.isSuccess() && !CollectionUtils.isEmpty(listRes.getResult())) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Fetch data {}",
                                            listRes.getResult().stream()
                                                    .map(SkuStockTask::getId).collect(Collectors.toList()));
                                }

                                listRes.getResult().forEach(
                                        skuStockTask -> skuStockThridExecutor.submit(new SotckPushTask(skuStockTask))
                                );
                            }
                        }
                    }
                    Thread.sleep(60000);

                }
            } catch (Exception e) {
                log.warn("fail to process sku stock to third db, cause:{}", Throwables.getStackTraceAsString(e));
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

        log.debug("start handle sku stock task (id:{}) to third db", skuStockTask.getId());

        List<StockDto> stockDtos = skuStockTask.getStockDtoList();

        try {
            //触发库存推送
            List<String> skuCodes = Lists.newArrayList();
            for (StockDto stockDto : stockDtos) {
                skuCodes.add(stockDto.getSkuCode());
            }
            stockPusher.submit(skuCodes);

            log.debug("end handle sku stock task (id:{}) to third db", skuStockTask.getId());
        } catch (Exception e) {
            log.error("failed to sync stocks task(id:{}) to third db, data:{},cause:{}", skuStockTask.getId(), stockDtos, Throwables.getStackTraceAsString(e));
        }

        log.debug("start delete sku stock task to third db (id:{})", skuStockTask.getId());
        Response<Boolean> updateRes = skuStockTaskWriteService.updateStatusById(skuStockTask.getId(),4);
        if (!updateRes.isSuccess()) {
            log.error("update sku stock task by id:{} to third db over fail,error:{}", skuStockTask.getId(), updateRes.getError());
        }

    }

    /**
     * @Description 是否在全量推送第三方时间段内
     * @Date        2018/5/29
     * @param
     * @return
     */
    private boolean isFullRunTime(){
        java.time.LocalTime localTime = java.time.LocalTime.now();
        java.time.LocalTime startTime = java.time.LocalTime.parse(stockSyncThirdFullTimeStart);
        java.time.LocalTime endTime = java.time.LocalTime.parse(stockSyncThirdFullTimeEnd);
        if(localTime.compareTo(startTime)>0 && localTime.compareTo(endTime)<0){
            return true;
        }
        return false;
    }
}
