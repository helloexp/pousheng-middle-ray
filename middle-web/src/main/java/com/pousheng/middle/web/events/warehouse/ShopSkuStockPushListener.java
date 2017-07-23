package com.pousheng.middle.web.events.warehouse;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.web.events.trade.HkShipmentDoneEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 推送一个店铺所有的sku库存
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-21
 */
@Slf4j
@Component
public class ShopSkuStockPushListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private MappingReadService mappingReadService;

    @Autowired
    private StockPusher stockPusher;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onPushEvent(PushEvent event){
        Long shopId = event.getShopId();
        int pageNo = 1;
        int pageSize = 100;
        while(true) {
            Response<Paging<ItemMapping>> r = mappingReadService.findByOpenShopId(shopId, pageNo, pageSize);
            if(!r.isSuccess()){
                log.error("failed to find pushed items by shopId(id={}), error code:{}", shopId, r.getError());
                return;
            }
            Paging<ItemMapping> p = r.getResult();
            List<ItemMapping> data = p.getData();
            for (ItemMapping datum : data) {
                stockPusher.submit(datum.getSkuCode());
            }
            pageNo++;
            if(data.size()<pageSize){
                return;
            }
        }
    }



}
