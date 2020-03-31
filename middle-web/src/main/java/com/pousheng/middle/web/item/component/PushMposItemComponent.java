package com.pousheng.middle.web.item.component;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.enums.PsSpuType;
import com.pousheng.middle.item.service.PsSkuTemplateWriteService;
import com.pousheng.middle.web.item.batchhandle.BatchHandleMposLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.event.OpenClientPushItemSuccessEvent;
import io.terminus.open.client.center.item.component.ItemExecutor;
import io.terminus.open.client.center.item.dto.ParanaFullItem;
import io.terminus.open.client.center.item.dto.ParanaItem;
import io.terminus.open.client.center.item.dto.ParanaSku;
import io.terminus.open.client.center.item.dto.ParanaSkuAttribute;
import io.terminus.open.client.center.item.service.ItemServiceCenter;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.mappings.service.MappingWriteService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.item.dto.ItemResult;
import io.terminus.open.client.item.dto.OpenClientSkuAttribute;
import io.terminus.open.client.item.model.PushedItem;
import io.terminus.open.client.item.service.PushedItemWriteService;
import io.terminus.open.client.parana.component.ParanaClient;
import io.terminus.open.client.parana.dto.ParanaCallResult;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 推送mpos商品
 * Created by songrenfei on 2018/1/10
 */
@Component
@Slf4j
public class PushMposItemComponent {

    @Autowired
    private MposParanaFullItemMaker mposParanaFullItemMaker;
    @Autowired
    private ItemServiceCenter itemServiceCenter;
    @Autowired
    private ItemExecutor itemExecutor;
    @Value("${mpos.open.shop.id}")
    private Long mposOpenShopId;
    @Autowired
    private ParanaClient paranaClient;
    @Autowired
    private OpenShopCacher openShopCacher;
    @RpcConsumer
    private MappingWriteService mappingWriteService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private MappingReadService mappingReadService;
    @RpcConsumer
    private PushedItemWriteService pushedItemWriteService;
    @Autowired
    private BatchHandleMposLogic batchHandleMposLogic;
    @Autowired
    private PsSkuTemplateWriteService psSkuTemplateWriteService;
    @Autowired
    private EventBus eventBus;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();



    public void push(SkuTemplate skuTemplate){
        itemExecutor.submit(new ItemPushTask(skuTemplate));
    }

    /**
     * 批量设置默认折扣
     * @param skuTemplates
     */
    public void batchMakeFlag(List<SkuTemplate> skuTemplates,Integer type){

        List<SkuTemplate> toUpdates = Lists.newArrayListWithCapacity(skuTemplates.size());
        for (SkuTemplate skuTemplate : skuTemplates) {
            toUpdates.add(makeUpdateSku(skuTemplate,type));
            //itemExecutor.submit(new ItemSetDiscountTask(skuTemplate,type));
        }
        Response<Boolean> updateRes = psSkuTemplateWriteService.updateBatch(toUpdates);
        if(!updateRes.isSuccess()){
            log.error("batch update sku template fail,error:{} ",updateRes.getError());
        }
    }

    private class ItemSetDiscountTask implements  Runnable {
        private final SkuTemplate skuTemplate;

        private final Integer type;

        public ItemSetDiscountTask(SkuTemplate skuTemplate,Integer type) {
            this.skuTemplate = skuTemplate;
            this.type = type;
        }

        @Override
        public void run() {
            makeFlagAndSetDiscount(skuTemplate,type);
        }
    }


    /**
     * 打标并设置默认折扣
     * @param exist        商品
     * @param type      打标/取消打标
     */
    public Response<Boolean> makeFlagAndSetDiscount(SkuTemplate exist,Integer type){
        log.info("sku(id:{}) make flag start..",exist.getId());

        SkuTemplate toUpdate = makeUpdateSku(exist,type);
        //这里的折扣默认都是1
        Response<Boolean> resp = psSkuTemplateWriteService.updateTypeAndExtraById(toUpdate.getId(),toUpdate.getType(),toUpdate.getPrice(),toUpdate.getExtraJson());
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate(id:{}) failed error={}",toUpdate.getId(),resp.getError());
            return Response.fail(resp.getError());
        }
        log.info("sku(id:{}) make flag over..",exist.getId());
        return Response.ok();
    }

    private SkuTemplate makeUpdateSku(SkuTemplate exist,Integer type){

        Integer discount = 100;
        Map<String,String> extra = exist.getExtra();
        if(CollectionUtils.isEmpty(extra)){
            extra = Maps.newHashMap();
        }
        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setType(type);
        //如果本来不包含折扣，默认设置折扣
        if(Objects.equals(type, PsSpuType.MPOS.value()) && !extra.containsKey(PsItemConstants.MPOS_DISCOUNT)){
            extra.put(PsItemConstants.MPOS_DISCOUNT,discount.toString());
            toUpdate.setExtra(extra);
            Integer originPrice = 0;
            if (exist.getExtraPrice() != null&&exist.getExtraPrice().containsKey(PsItemConstants.ORIGIN_PRICE_KEY)) {
                originPrice = exist.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY);
            }

            if(Objects.equals(originPrice,0)){
                log.error("[PRICE-INVALID]:sku template:(id:{}) price  code:{} invalid",exist.getId(),exist.getSkuCode());
            }

            toUpdate.setPrice(originPrice);
        }

        return toUpdate;

    }



    private class ItemPushTask implements Runnable {

        private final SkuTemplate skuTemplate;

        private ItemPushTask(SkuTemplate skuTemplate) {
            this.skuTemplate = skuTemplate;
        }

        @Override
        public void run() {
            syncParanaMposItem(skuTemplate);
        }
    }


    public Response<Boolean> syncParanaMposItem(SkuTemplate skuTemplate){
        ParanaFullItem paranaFullItem = mposParanaFullItemMaker.make(skuTemplate);
        ParanaItem paranaItem = paranaFullItem.getItem();

        Response<ItemResult> addR = itemServiceCenter.addItem(mposOpenShopId, paranaFullItem);
        if (!addR.isSuccess()) {
            log.error("fail to add item:{} for openShop(id={}),cause:{}", paranaFullItem, mposOpenShopId, addR.getError());
            return Response.fail(addR.getError());
        }else {
            OpenShop openShop = openShopCacher.findById(mposOpenShopId);
            Map<String, List<ParanaSkuAttribute>> paranaSkuAttributesBySkuCode = Maps.newHashMapWithExpectedSize(paranaFullItem.getSkus().size());
            for (ParanaSku paranaSku : paranaFullItem.getSkus()) {
                paranaSkuAttributesBySkuCode.put(paranaSku.getSkuCode(), paranaSku.getSkuAttributes());
            }

            ItemResult itemResult = addR.getResult();
            List<ItemMapping> createdItemMappings = Lists.newArrayListWithCapacity(itemResult.getOpenSkuIds().size());
            for (Map.Entry<String, String> entry : itemResult.getOpenSkuIds().entrySet()) {
                ItemMapping itemMapping = new ItemMapping();
                itemMapping.setChannel(openShop.getChannel());
                itemMapping.setOpenShopId(openShop.getId());
                itemMapping.setOpenShopName(openShop.getShopName());
                itemMapping.setItemId(paranaFullItem.getItem().getItemId());
                itemMapping.setItemName(paranaFullItem.getItem().getName());
                itemMapping.setChannelItemId(itemResult.getOpenItemId());
                itemMapping.setSkuCode(entry.getKey());
                itemMapping.setChannelSkuId(entry.getValue());
                itemMapping.setStatus(1);

                List<ParanaSkuAttribute> paranaSkuAttributes = paranaSkuAttributesBySkuCode.get(entry.getKey());
                if (!CollectionUtils.isEmpty(paranaSkuAttributes)) {
                    List<OpenClientSkuAttribute> skuAttributes = Lists.newArrayListWithCapacity(paranaSkuAttributes.size());
                    for (ParanaSkuAttribute paranaSkuAttribute : paranaSkuAttributes) {
                        OpenClientSkuAttribute openClientSkuAttribute = new OpenClientSkuAttribute();
                        openClientSkuAttribute.setAttributeKeyId(paranaSkuAttribute.getAttributeKeyId());
                        openClientSkuAttribute.setAttributeKey(paranaSkuAttribute.getAttributeKey());
                        openClientSkuAttribute.setAttributeValueId(paranaSkuAttribute.getAttributeValueId());
                        openClientSkuAttribute.setAttributeValue(paranaSkuAttribute.getAttributeValue());
                        skuAttributes.add(openClientSkuAttribute);
                    }
                    itemMapping.setSkuAttributes(skuAttributes);
                }

                //判断是否已经存在
                if(mappingIsExist(itemMapping)){
                    continue;
                }
                createdItemMappings.add(itemMapping);
            }
            Response<Boolean> createItemMappingsR = mappingWriteService.createItemMappings(createdItemMappings);
            if (!createItemMappingsR.isSuccess()) {
                log.error("fail to create item mappings:{},cause:{}", createdItemMappings, createItemMappingsR.getError());
            }

            //触发同步恒康电商在售
            eventBus.post(new OpenClientPushItemSuccessEvent(openShop.getId(), skuTemplate.getSpuId()));


            PushedItem pushedItem = new PushedItem();
            pushedItem.setChannel(openShop.getChannel());
            pushedItem.setOpenShopId(openShop.getId());
            pushedItem.setOpenShopName(openShop.getShopName());
            pushedItem.setItemId(paranaItem.getItemId());
            pushedItem.setItemName(paranaItem.getName());
            if (addR.isSuccess()) {
                pushedItem.setStatus(1);
                pushedItem.setSyncOpenIdStatus(itemResult.isNeedSyncOpenId() ? 0 : 1);
                pushedItem.setChannelItemId(itemResult.getOpenItemId());
                pushedItem.setCause("");
            } else {
                pushedItem.setStatus(-1);
                pushedItem.setSyncOpenIdStatus(-1);
                pushedItem.setCause(addR.getError());
            }
            createOrUpdatePushedItem(pushedItem);
        }

        return Response.ok();

    }


    private Boolean mappingIsExist(ItemMapping itemMapping){
        Optional<ItemMapping> optional = findMapping(itemMapping.getSkuCode(),itemMapping.getOpenShopId());
        if(optional.isPresent()){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Optional<ItemMapping> findMapping(String skuCode,Long mposOpenShopId){
        Response<Optional<ItemMapping>> response = mappingReadService.findBySkuCodeAndOpenShopId(skuCode,mposOpenShopId);
        if(!response.isSuccess()){
            log.error("find item mapping by sku code:{} open shop id:{} fail,error:{}",skuCode,mposOpenShopId,response.getError());
            throw new ServiceException(response.getError());
        }

        return response.getResult();
    }



    private void createOrUpdatePushedItem(PushedItem pushedItem) {
        Response<Boolean> r = pushedItemWriteService.createOrUpdate(pushedItem);
        if (!r.isSuccess()) {
            log.error("fail to create pushed item:{},cause:{}", pushedItem, r.getError());
        }
    }




    public void del(List<SkuTemplate> skuTemplates){
        List<String> skuCodes = Lists.transform(skuTemplates, new Function<SkuTemplate, String>() {
            @Nullable
            @Override
            public String apply(@Nullable SkuTemplate input) {
                return input.getSkuCode();
            }
        });

        Map<String,Object> params = Maps.newHashMap();
        try {
            params.put("skuCodes", Joiners.COMMA.join(skuCodes));
            ParanaCallResult result = mapper.fromJson(paranaClient.systemPost("del.mpos.sku.api",params),ParanaCallResult.class);
            if(!result.getSuccess()){
                log.error("sync del mpos skuTemplates:{} to parana fail,error:{}",skuTemplates, result.getError());
                return;
            }

            //删除映射关系
            delItemMapping(skuTemplates);

        }catch (Exception e){
            log.error("sync del mpos skuTemplate:{} to parana fail,cause:{}",skuTemplates, Throwables.getStackTraceAsString(e));
        }
    }

    private void delItemMapping(List<SkuTemplate> skuTemplates){
        for (SkuTemplate skuTemplate : skuTemplates){
            Optional<ItemMapping> optional = findMapping(skuTemplate.getSkuCode(),mposOpenShopId);
            if(!optional.isPresent()){
                log.error("not find item mapping by sku code :{} and open shop id:{}",skuTemplate.getSkuCode(),mposOpenShopId);
                continue;
            }
            ItemMapping itemMapping = optional.get();
            mappingWriteService.delete(itemMapping.getId());
        }
    }

    public void updatePrice(List<SkuTemplate> skuTemplates,Integer price){
        List<String> skuCodes = Lists.transform(skuTemplates, new Function<SkuTemplate, String>() {
            @Nullable
            @Override
            public String apply(@Nullable SkuTemplate input) {
                return input.getSkuCode();
            }
        });

        Map<String,Object> params = Maps.newHashMap();
        try {
            params.put("skuCodes", Joiners.COMMA.join(skuCodes));
            params.put("price", price);
            ParanaCallResult result = mapper.fromJson(paranaClient.systemPost("update.mpos.sku.price.api",params),ParanaCallResult.class);
            if(!result.getSuccess()){
                log.error("sync mpos skuTemplates:{} price:{} to parana fail,error:{}",skuTemplates, price,result.getError());
            }
        }catch (Exception e){
            log.error("sync mpos skuTemplate:{} price:{} to parana fail,cause:{}",skuTemplates,price, Throwables.getStackTraceAsString(e));
        }
    }


    public void updateImage(List<SkuTemplate> skuTemplates,String imageUrl){
        List<String> skuCodes = Lists.transform(skuTemplates, new Function<SkuTemplate, String>() {
            @Nullable
            @Override
            public String apply(@Nullable SkuTemplate input) {
                return input.getSkuCode();
            }
        });

        Map<String,Object> params = Maps.newHashMap();
        try {
            params.put("skuCodes", Joiners.COMMA.join(skuCodes));
            params.put("image", imageUrl);
            ParanaCallResult result = mapper.fromJson(paranaClient.systemPost("update.mpos.sku.image.api",params),ParanaCallResult.class);
            if(!result.getSuccess()){
                log.error("sync mpos skuTemplates:{} image:{} to parana fail,error:{}",skuTemplates, imageUrl,result.getError());
            }
        }catch (Exception e){
            log.error("sync mpos skuTemplate:{} image:{} to parana fail,cause:{}",skuTemplates,imageUrl, Throwables.getStackTraceAsString(e));
        }
    }





}
