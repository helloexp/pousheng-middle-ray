package com.pousheng.middle.web.events.item.listener;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.events.item.BatchSyncStockEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/7
 */
@Slf4j
@Component
public class BatchSyncStockListener {


    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;

    @Autowired
    private StockPusher stockPusher;





    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onBatchSyncStockEvent(BatchSyncStockEvent event){

        log.debug("batch sync stock to middle start ,total:{}",event);

        List<StockDto> stockDtos = event.getStockDtos();
        try {
            Response<Boolean> r = warehouseSkuWriteService.syncStock(stockDtos);
            if(!r.isSuccess()){
                log.error("failed to stocks, data:{},error:{}", stockDtos, r.getError());
                return;
            }
            //触发库存推送
            List<String> skuCodes = Lists.newArrayList();
            for (StockDto stockDto : stockDtos) {
                skuCodes.add(stockDto.getSkuCode());
            }
            stockPusher.submit(skuCodes);
        } catch (Exception e) {
            log.error("failed to sync stocks, data:{},cause:{}", stockDtos, Throwables.getStackTraceAsString(e));
        }

        log.debug("batch sync stock to middle end");

    }








}
