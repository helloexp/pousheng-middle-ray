package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.web.events.warehouse.PushEvent;
import com.pousheng.middle.web.item.cacher.GroupRuleCacherProxy;
import com.pousheng.middle.web.mq.warehouse.InventoryChangeProducer;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    @Autowired
    private OpenShopReadService openShopReadService;

    @Autowired
    private GroupRuleCacherProxy groupRuleCacherProxy;

    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

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

        Response<OpenShop> openShopResponse = openShopReadService.findById(shopId);
        if (!openShopResponse.isSuccess()) {
            log.error("failed to find openShop(shopId={}),error code:{}", shopId, openShopResponse.getError());
            throw new ServiceException("openShop.find.fail");
        }
        //云聚JIt店铺库存没有商品映射关系，推送集合为店铺商品分组
        if(Objects.equals(openShopResponse.getResult().getChannel(),MiddleChannel.YUNJUJIT.getValue())){
            this.pushYunjuJIT(shopId, pageNo, pageSize);
        }else{
            this.push(shopId, pageNo, pageSize);
        }
    }

    private void pushYunjuJIT(Long shopId, int pageNo, int pageSize){
        Set<Long> groupIds = Sets.newHashSet(groupRuleCacherProxy.findByShopId(shopId));
        log.info("query shop group ,shopId{}, groupIds", shopId, groupIds);

        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("groupIds", Joiners.COMMA.join(groupIds));
        while(true) {
            Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(pageNo, pageSize, templateName, params, SearchSkuTemplate.class);
            if (!response.isSuccess()) {
                log.error("query sku template by groupIds:{} fail,error:{}", groupIds, response.getError());
                throw new JsonResponseException(response.getError());
            }
            List<SearchSkuTemplate> skus = response.getResult().getData();

            if(log.isDebugEnabled()){
                log.debug("query shop group items({}) for yunju jit shop", skus.toString());
            }

            List<String> skuCodes = Lists.newArrayList();
            skus.forEach(searchSkuTemplate -> {
                        String skuCode = searchSkuTemplate.getSkuCode();
                        if (!Objects.isNull(skuCode) && !Objects.equals(skuCode, "")) {
                            skuCodes.add(skuCode);
                        }
                    }
            );

            if(log.isDebugEnabled()){
                log.debug("stock skuCodes({}) for yunju jit shop ", skuCodes.toString());
            }

            List<InventoryChangeDTO> inventoryChanges = com.google.common.collect.Lists.newArrayList();
            skuCodes.forEach(skuCode ->{
                inventoryChanges.add(InventoryChangeDTO.builder().skuCode(skuCode).warehouseId(null).build());
            });
            inventoryChangeProducer.handleInventoryChange(inventoryChanges);

            log.info("push stock pageNo is {}",pageNo);
            pageNo++;
            if(skus.size()<pageSize){
                log.info("push stock return pageNo is {}",pageNo);
                return;
            }
        }
    }

    private void push(Long shopId, int pageNo, int pageSize){
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
