/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.impl.service;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.item.dto.IndexedSkuTemplate;
import com.pousheng.middle.item.service.IndexedSkuTemplateFactory;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.attribute.dto.GroupedOtherAttribute;
import io.terminus.parana.attribute.dto.OtherAttribute;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.cache.BrandCacher;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.model.SpuAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Author:  songrenfei
 * Date: 2017-11-31
 */
@Slf4j
public class DefaultIndexedSkuTemplateFactory implements IndexedSkuTemplateFactory {

    protected final BackCategoryCacher backCategoryCacher;


    protected final BrandCacher brandCacher;



    @SuppressWarnings("unchecked")
    public DefaultIndexedSkuTemplateFactory(final BackCategoryCacher backCategoryCacher,
                                            BrandCacher brandCacher) {
        this.backCategoryCacher = backCategoryCacher;
        this.brandCacher = brandCacher;
    }

    /**
     * 创建dump到搜索引擎的商品对象, 包含属性, 类目等信息
     */
    public IndexedSkuTemplate create(SkuTemplate skuTemplate, Spu spu, SpuAttribute spuAttribute, Object... others) {

        Map<String,String> extra = skuTemplate.getExtra();
        String materialId = extra.get("materialId");
        IndexedSkuTemplate indexedSkuTemplate = new IndexedSkuTemplate();
        indexedSkuTemplate.setId(skuTemplate.getId());
        indexedSkuTemplate.setName(skuTemplate.getName());
        indexedSkuTemplate.setMainImage(skuTemplate.getImage_());
        indexedSkuTemplate.setSpuId(spu.getId());
        indexedSkuTemplate.setSpuCode(materialId);//货号
        indexedSkuTemplate.setSkuCode(skuTemplate.getSkuCode());
        Integer type = Arguments.isNull(skuTemplate.getType())?0:skuTemplate.getType();
        indexedSkuTemplate.setType(type);
        indexedSkuTemplate.setUpdatedAt(skuTemplate.getUpdatedAt());
        final Long brandId = spu.getBrandId();
        if (brandId != null) {
            Brand brand = brandCacher.findBrandById(brandId);
            indexedSkuTemplate.setBrandId(brandId);
            indexedSkuTemplate.setBrandName(brand.getName());
        }

        final Long categoryId = spu.getCategoryId();
        BackCategory currentBackCategory = backCategoryCacher.findBackCategoryById(categoryId);
        //List<BackCategory> backCategories = backCategoryCacher.findAncestorsOf(categoryId);
        List<Long> backCategoryIds = Lists.newArrayListWithCapacity(1);
        /*for (BackCategory backCategory : backCategories) {
            backCategoryIds.add(backCategory.getId());
        }*/

        backCategoryIds.add(categoryId);

        indexedSkuTemplate.setCategoryIds(backCategoryIds);
        indexedSkuTemplate.setCategoryName(currentBackCategory.getName());


        //非销售属性
        List<String> attributes = Lists.newArrayList();
        List<GroupedOtherAttribute> otherAttributes = spuAttribute.getOtherAttrs();
        if (!CollectionUtils.isEmpty(otherAttributes)) {
            for (GroupedOtherAttribute groupedOtherAttribute : otherAttributes) {
                for (OtherAttribute attr : groupedOtherAttribute.getOtherAttributes()) {
                    if (isSearchableAttribute(categoryId, attr.getAttrKey())) {
                        if(Objects.equal(attr.getAttrKey(),"年份")||Objects.equal(attr.getAttrKey(),"季节")){
                            attributes.add(attr.getAttrKey() + ":" + attr.getAttrVal());
                        }
                    }
                }
            }
        }


        //销售属性
        List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
        if (!CollectionUtils.isEmpty(skuAttributes)) {
            for (SkuAttribute skuAttribute : skuAttributes) {
                if(Objects.equal(skuAttribute.getAttrKey(),"尺码")) {
                    attributes.add(skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal());
                }
            }
        }
        indexedSkuTemplate.setAttributes(attributes);

        //indexedSkuTemplate.setPrice(skuTemplate.getPrice());

        return indexedSkuTemplate;
    }

    /**
     * 检查指定类目下的属性是否可搜索
     *
     * @param categoryId   后台类目id
     * @param attributeKey 属性名
     * @return 可搜索返回true，否则返回false
     */
    protected boolean isSearchableAttribute(Long categoryId, String attributeKey) {
        /*List<CategoryAttribute> attributes = categoryAttributeCacher.findByCategoryId(categoryId);
        for (CategoryAttribute attribute : attributes) {
            if (!Objects.equal(attribute.getAttrKey(), attributeKey)) {
                continue;
            }

            Map<AttributeMetaKey, String> attrMetas = attribute.getAttrMetas();
            if (attrMetas == null || !attrMetas.containsKey(AttributeMetaKey.SEARCHABLE)) {
                return true;
            }

            return Boolean.valueOf(attrMetas.get(AttributeMetaKey.SEARCHABLE));
        }*/
        return true;
    }


}
