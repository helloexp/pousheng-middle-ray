package com.pousheng.middle.web.warehouses;

import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.web.events.warehouse.PushEvent;
import com.pousheng.middle.web.mq.warehouse.InventoryChangeProducer;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
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

import java.util.List;

/**
 * 推送一个店铺所有的sku库存，该类由ShopSkuStockPushListener改写，由通过EventBus改为同步调用
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2018-07-18
 */
@Slf4j
@Component
public class ShopSkuStockPushHandler {

    @RpcConsumer
    private MappingReadService mappingReadService;

    @Autowired
    private StockPusher stockPusher;

    @Autowired
    private InventoryChangeProducer inventoryChangeProducer;

    public void onPushEvent(PushEvent event){

        if (null != event && StringUtils.isNotBlank(event.getSkuCode())) {
            log.info("begin to push stock for skuCode(skuCode={})", event.getSkuCode());
            //stockPusher.submit(Lists.newArrayList(event.getSkuCode()));
            String skuCode = event.getSkuCode();
            inventoryChangeProducer.handleInventoryChange(InventoryChangeDTO.builder().skuCode(skuCode).build());
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
                log.info("trying to push stock of sku(code={})", datum.getSkuCode());
                skuCodes.add(datum.getSkuCode());
            }
            //stockPusher.submit(skuCodes);
            List<InventoryChangeDTO> inventoryChanges = com.google.common.collect.Lists.newArrayList();
            skuCodes.forEach(skuCode ->{
                inventoryChanges.add(InventoryChangeDTO.builder().skuCode(skuCode).warehouseId(null).build());
            });
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
