package com.pousheng.middle.web.events.warehouse;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.stock.StockPusherLogic;
import com.pousheng.middle.web.mq.warehouse.InventoryChangeProducer;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import com.pousheng.middle.web.warehouses.ShopSkuStockPushHandler;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

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

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Autowired
    private InventoryChangeProducer inventoryChangeProducer;

    @Autowired
    private StockPusherLogic stockPusherLogic;

    @Autowired
    private ShopSkuStockPushHandler shopSkuStockPushHandler;


    @Subscribe
    public void onPushEvent(PushEvent event){
        if (null != event && StringUtils.isNotBlank(event.getSkuCode())) {
            log.info("begin to push stock for skuCode(skuCode={})", event.getSkuCode());
            //stockPusher.submit(Lists.newArrayList(event.getSkuCode()));
            String skuCode = event.getSkuCode();
            inventoryChangeProducer.handleInventoryChange(InventoryChangeDTO.builder().skuCode(skuCode).warehouseId(null).build());
            return;
        }

        Long shopId = event.getShopId();
        log.info("begin to push stock for shop(id={})", shopId);
        int pageNo = 1;
        int pageSize = 100;
        while(true) {
            Response<Paging<ItemMapping>> r = mappingReadService.findByOpenShopId(shopId, null,pageNo, pageSize);
            if(!r.isSuccess()){
                log.error("failed to find pushed items by shopId(id={}), error code:{}", shopId, r.getError());
                return;
            }
            Paging<ItemMapping> p = r.getResult();
            List<ItemMapping> data = p.getData();
            List<String> skuCodes = Lists.newArrayList();
            for (ItemMapping datum : data) {
                String skuCode = datum.getSkuCode();
                if(!Objects.isNull(skuCode)&&!Objects.equals(skuCode,"")) {
                    skuCodes.add(skuCode);
                }
            }
            //stockPusher.submit(skuCodes);

            List<InventoryChangeDTO> inventoryChanges = shopSkuStockPushHandler.buildChangeList(skuCodes,shopId);

            inventoryChangeProducer.handleInventoryChange(inventoryChanges);

            log.info("push stock pageNo is {}",pageNo);
            pageNo++;
            if(data.size()<pageSize){
                log.info("push stock return pageNo is {}",pageNo);
                return;
            }
        }
    }
}
