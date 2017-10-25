package com.pousheng.middle.open.api;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.web.events.item.BatchSyncStockEvent;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 恒康主动推sku的库存过来
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-10
 */
@OpenBean
@Slf4j
public class WarehouseStockApi {


    @Autowired
    private EventBus eventBus;


    @OpenMethod(key = "hk.stock.api", paramNames = {"total", "data"}, httpMethods = RequestMethod.POST)
    public void onStockChanged(@RequestParam("total")Integer total, @RequestParam("data")String data){
        log.info("ERPSTOCK -- begin to handle erp stock:{} , total:{}", data,total);
        BatchSyncStockEvent syncStockEvent = new BatchSyncStockEvent();
        syncStockEvent.setTotal(total);
        syncStockEvent.setData(data);
        eventBus.post(syncStockEvent);

    }

}
