package com.pousheng.middle.open;

import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.center.item.api.ParanaFullItemMaker;
import io.terminus.open.client.center.item.dto.*;
import io.terminus.parana.attribute.dto.GroupedOtherAttribute;
import io.terminus.parana.attribute.dto.OtherAttribute;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.item.dto.ImageInfo;
import io.terminus.parana.spu.dto.FullSpu;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.model.SpuDetail;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by cp on 6/19/17.
 */
@Component
@Slf4j
public class PushedItemMaker implements ParanaFullItemMaker {

    @RpcConsumer
    private SpuReadService spuReadService;

    @Autowired
    private BackCategoryCacher backCategoryCacher;

    private final static String ORIGIN_PRICE_KEY = "originPrice";

    @Override
    public ParanaFullItem make(Long spuId) {
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
        paranaItem.setItemCode(spu.getId().toString());
        paranaItem.setAdvertise(spu.getAdvertise());
        paranaItem.setBrandId(spu.getBrandId());
        paranaItem.setName(spu.getName());
        paranaItem.setCategoryId(spu.getCategoryId());
        if (Arguments.notNull(spuDetail.getImages())) {
            paranaItem.setImages(spuDetail.getImages().stream().filter(Objects::nonNull)
                    .map(ImageInfo::getUrl).collect(Collectors.toList()));
        }
        paranaItem.setDetail(spuDetail.getDetail());

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
        for (SkuTemplate skuTemplate : skuTemplates) {
            ParanaSku paranaSku = new ParanaSku();
            if (skuTemplate.getExtraPrice() != null) {
                paranaSku.setMarketPrice(skuTemplate.getExtraPrice().get(ORIGIN_PRICE_KEY));
            } else {
                paranaSku.setMarketPrice(skuTemplate.getPrice());
            }
            paranaSku.setPrice(paranaSku.getMarketPrice());
            paranaSku.setStockQuantity(0);
            paranaSku.setImage(skuTemplate.getImage());
            paranaSku.setSkuCode(skuTemplate.getSkuCode());

            final List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
            if (!CollectionUtils.isEmpty(skuAttributes)) {
                List<ParanaSkuAttribute> paranaSkuAttributes = Lists.newArrayListWithCapacity(skuAttributes.size());
                for (SkuAttribute skuAttribute : skuAttributes) {
                    ParanaSkuAttribute paranaSkuAttribute = new ParanaSkuAttribute();
                    paranaSkuAttribute.setAttributeKeyId(skuAttribute.getAttrKey());
                    paranaSkuAttribute.setAttributeValueId(skuAttribute.getAttrVal());
                    paranaSkuAttributes.add(paranaSkuAttribute);
                }
                paranaSku.setSkuAttributes(paranaSkuAttributes);
            }

            paranaSkus.add(paranaSku);
        }

        ParanaFullItem paranaFullItem = new ParanaFullItem();
        paranaFullItem.setItem(paranaItem);
        paranaFullItem.setSkus(paranaSkus);
        return paranaFullItem;
    }


}
