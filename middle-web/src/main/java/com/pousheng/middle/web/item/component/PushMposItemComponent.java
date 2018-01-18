package com.pousheng.middle.web.item.component;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.item.dto.ParanaFullItem;
import io.terminus.open.client.center.item.dto.ParanaSku;
import io.terminus.open.client.center.item.dto.ParanaSkuAttribute;
import io.terminus.open.client.center.item.service.ItemServiceCenter;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingWriteService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.item.dto.ItemResult;
import io.terminus.open.client.item.dto.OpenClientSkuAttribute;
import io.terminus.open.client.parana.component.ParanaClient;
import io.terminus.open.client.parana.dto.ParanaCallResult;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

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
    @Value("${mpos.open.shop.id}")
    private Long mposOpenShopId;
    @Autowired
    private ParanaClient paranaClient;
    @Autowired
    private OpenShopCacher openShopCacher;
    @RpcConsumer
    private MappingWriteService mappingWriteService;
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();




    public void push(SkuTemplate skuTemplate){

        ParanaFullItem paranaFullItem = mposParanaFullItemMaker.make(skuTemplate);
        Response<ItemResult> addR = itemServiceCenter.addItem(mposOpenShopId, paranaFullItem);
        if (!addR.isSuccess()) {
            log.error("fail to add item:{} for openShop(id={}),cause:{}", paranaFullItem, mposOpenShopId, addR.getError());
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
                createdItemMappings.add(itemMapping);
            }
            Response<Boolean> createItemMappingsR = mappingWriteService.createItemMappings(createdItemMappings);
            if (!createItemMappingsR.isSuccess()) {
                log.error("fail to create item mappings:{},cause:{}", createdItemMappings, createItemMappingsR.getError());
            }
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
            }
        }catch (Exception e){
            log.error("sync del mpos skuTemplate:{} to parana fail,cause:{}",skuTemplates, Throwables.getStackTraceAsString(e));
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
