package com.pousheng.middle.web.item.rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.parana.attribute.dto.AttributeMetaKey;
import io.terminus.parana.attribute.dto.GroupedSkuAttribute;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.component.dto.attribute.GroupedSkuAttributeWithRule;
import io.terminus.parana.component.dto.attribute.SkuAttributeRule;
import io.terminus.parana.rule.RuleExecutor;
import io.terminus.parana.rule.dto.BaseInput;
import io.terminus.parana.rule.dto.BaseOutput;
import io.terminus.parana.spu.dto.FullSpu;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-24
 */
public class PoushengSpuSkuAttributeRule extends RuleExecutor {
    @Override
    public void doHandleInboundData(BaseInput baseInput, BaseOutput baseOutput) throws InvalidException {
    }

    @Override
    public void doHandleOutboundData(BaseInput input, BaseOutput result) {
        final List<GroupedSkuAttribute> groupedSkuAttributes = input.getGroupedSkuAttributes();

        List<GroupedSkuAttributeWithRule> groupedSkuAttributeWithRules = Lists.newArrayList();

        final Map<AttributeMetaKey, String> attrMetas = Maps.newHashMap();
        attrMetas.put(AttributeMetaKey.REQUIRED, "1");
        attrMetas.put(AttributeMetaKey.IMPORTANT, "1");
        attrMetas.put(AttributeMetaKey.VALUE_TYPE, "STRING");
        attrMetas.put(AttributeMetaKey.SKU_CANDIDATE, "1");
        attrMetas.put(AttributeMetaKey.PSEUDO_SKU_CANDIDATE, "1");
        attrMetas.put(AttributeMetaKey.SEARCHABLE, "0");
        attrMetas.put(AttributeMetaKey.USER_DEFINED, "1");


        //校验每个销售属性是否合法
        for (GroupedSkuAttribute groupedSkuAttribute : groupedSkuAttributes) {

            List<SkuAttributeRule> skuAttributeRules = Lists.newArrayList();
            for (SkuAttribute skuAttribute : groupedSkuAttribute.getSkuAttributes()) {
                //如果属性值在预设值范围类,或者属性允许自定义, 则为有效的销售属性
                String attrVal = skuAttribute.getAttrVal();

                SkuAttributeRule skuAttributeRule = new SkuAttributeRule();
                skuAttributeRule.setAttrMetas(attrMetas);
                skuAttributeRule.setAttrVal(attrVal);
                skuAttributeRule.setUnit(skuAttribute.getUnit());
                skuAttributeRule.setImage(skuAttribute.getImage());
                skuAttributeRule.setShowImage(skuAttribute.getShowImage());
                skuAttributeRule.setThumbnail(skuAttribute.getThumbnail());
                skuAttributeRules.add(skuAttributeRule);

            }
            if (!CollectionUtils.isEmpty(skuAttributeRules)) {
                GroupedSkuAttributeWithRule gsaw = new GroupedSkuAttributeWithRule();
                gsaw.setAttrKey(groupedSkuAttribute.getAttrKey());
                gsaw.setAttributeRules(skuAttributeRules);
                groupedSkuAttributeWithRules.add(gsaw);
            }
        }

        result.setSkuAttrs(groupedSkuAttributeWithRules);

    }

    @Override
    public boolean support(BaseInput input) {
        return input instanceof FullSpu;
    }

    @Override
    protected List<?> getRules(BaseInput baseInput) {
        return Collections.emptyList();
    }
}
