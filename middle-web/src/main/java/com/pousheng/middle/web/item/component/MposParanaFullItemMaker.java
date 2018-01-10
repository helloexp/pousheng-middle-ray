package com.pousheng.middle.web.item.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.center.item.dto.*;
import io.terminus.parana.attribute.dto.GroupedOtherAttribute;
import io.terminus.parana.attribute.dto.OtherAttribute;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.item.dto.ImageInfo;
import io.terminus.parana.spu.dto.FullSpu;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.model.SpuDetail;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * mpos商品封装parana item
 * Created by songrenfei on 2018/1/10
 */
@Component
@Slf4j
public class MposParanaFullItemMaker {

    @RpcConsumer
    private SpuReadService spuReadService;

    private final static String ORIGIN_PRICE_KEY = "originPrice";

    public ParanaFullItem make(SkuTemplate skuTemplate){

        Response<Spu> spuRes = spuReadService.findById(skuTemplate.getSpuId());
        if(!spuRes.isSuccess()){
            log.error("find spu by id:{} fail,error:{}",skuTemplate.getSpuId(),spuRes.getError());
            throw new JsonResponseException(spuRes.getError());
        }

        Map<String,String> skuTemplateExtra = skuTemplate.getExtra();
        if(!skuTemplateExtra.containsKey("materialId")|| Strings.isNullOrEmpty(skuTemplateExtra.get("materialId"))){
            log.error("sku template(id:{}) materialId is invalid",skuTemplate.getId());
            throw new JsonResponseException("material.id.invalid");
        }
        String materialId = skuTemplateExtra.get("materialId");//货号
        Long spuId = skuTemplate.getSpuId();

        Response<FullSpu> rFullSpu = spuReadService.findFullInfoBySpuId(spuId);
        if (!rFullSpu.isSuccess()) {
            log.error("failed to find spu(id={}), error code:{}", spuId, rFullSpu.getError());
            throw new ServiceException(rFullSpu.getError());
        }
        final FullSpu fullSpu = rFullSpu.getResult();
        final Spu spu = fullSpu.getSpu();
        final SpuDetail spuDetail = fullSpu.getSpuDetail();
        final List<SkuTemplate> skuTemplates = fullSpu.getSkuTemplates();

        ParanaItem paranaItem = new ParanaItem();
        paranaItem.setItemId(spuId);
        paranaItem.setSpuId(spuId);
        paranaItem.setItemCode(materialId);
        paranaItem.setAdvertise(spu.getAdvertise());
        paranaItem.setBrandId(spu.getBrandId());
        paranaItem.setName(spu.getName());
        paranaItem.setMainImage(spu.getMainImage());
        paranaItem.setCategoryId(spu.getCategoryId());
        //添加型号
        paranaItem.setSpecification(spu.getSpecification());
        if (Arguments.notNull(spuDetail.getImages())) {
            paranaItem.setImages(spuDetail.getImages().stream().filter(Objects::nonNull)
                    .map(ImageInfo::getUrl).collect(Collectors.toList()));
        }
        paranaItem.setDetail(spuDetail.getDetail());

        Map<String, String> extra = Maps.newHashMap();
        extra.put("unit","件");
        extra.put("selfPlatformLink","");
        extra.put("unitQuantity","1");
        paranaItem.setExtra(extra);



        final List<GroupedOtherAttribute> groupedOtherAttributes = fullSpu.getGroupedOtherAttributes();
        if (!CollectionUtils.isEmpty(groupedOtherAttributes)) {
            List<ParanaItemAttribute> paranaItemAttributes = Lists.newArrayList();
            for (GroupedOtherAttribute groupedOtherAttribute : groupedOtherAttributes) {
                for (OtherAttribute otherAttribute : groupedOtherAttribute.getOtherAttributes()) {
                    ParanaItemAttribute paranaItemAttribute = new ParanaItemAttribute();
                    paranaItemAttribute.setAttributeKeyId(otherAttribute.getAttrKey());
                    paranaItemAttribute.setAttributeValueId(otherAttribute.getAttrVal());
                    paranaItemAttribute.setGroup(otherAttribute.getGroup());
                    paranaItemAttributes.add(paranaItemAttribute);
                }
            }
            paranaItem.setAttributes(paranaItemAttributes);
        }


        List<ParanaSku> paranaSkus = Lists.newArrayListWithCapacity(skuTemplates.size());
        ParanaSku paranaSku = new ParanaSku();
        paranaSku.setMarketPrice(skuTemplate.getPrice());
        paranaSku.setPrice(paranaSku.getMarketPrice());
        paranaSku.setStockQuantity(0);
        paranaSku.setImage(skuTemplate.getImage());
        paranaSku.setSkuCode(skuTemplate.getSkuCode());
        Map<String, String> skuExtra = Maps.newHashMap();
        skuExtra.put("unitQuantity","1");
        paranaSku.setExtra(skuExtra);

        final List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
        if (!CollectionUtils.isEmpty(skuAttributes)) {
            List<ParanaSkuAttribute> paranaSkuAttributes = Lists.newArrayListWithCapacity(skuAttributes.size());
            for (SkuAttribute skuAttribute : skuAttributes) {
                ParanaSkuAttribute paranaSkuAttribute = new ParanaSkuAttribute();
                paranaSkuAttribute.setAttributeKeyId(skuAttribute.getAttrKey());
                paranaSkuAttribute.setAttributeKey(skuAttribute.getAttrKey());
                paranaSkuAttribute.setAttributeValueId(skuAttribute.getAttrVal());
                paranaSkuAttribute.setAttributeValue(skuAttribute.getAttrVal());
                paranaSkuAttributes.add(paranaSkuAttribute);
            }
            paranaSku.setSkuAttributes(paranaSkuAttributes);
        }

        paranaSkus.add(paranaSku);

        ParanaFullItem paranaFullItem = new ParanaFullItem();
        paranaFullItem.setItem(paranaItem);
        paranaFullItem.setSkus(paranaSkus);
        return paranaFullItem;
    }

}
