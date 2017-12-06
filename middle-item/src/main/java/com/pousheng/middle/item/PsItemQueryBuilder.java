/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item;

import com.google.common.collect.Lists;
import io.terminus.parana.search.item.impl.DefaultItemQueryBuilder;
import io.terminus.search.api.query.Term;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author songrenfei
 */
@Slf4j
@Primary
@Component
public class PsItemQueryBuilder extends DefaultItemQueryBuilder {

    @Override
    public List<Term> buildTerm(Map<String, String> params) {
        List<Term> termList = Lists.newArrayList();
        String bid = params.get("bid"); // 搜索品牌
        if (StringUtils.hasText(bid)) {
            termList.add(new Term("brandId", bid));
        }

        String shopId = params.get("shopId"); // 店铺内搜索
        if (StringUtils.hasText(shopId)) {
            termList.add(new Term("shopId", shopId));
        }

        String skuCode = params.get("skuCode"); // 货品条码
        if (StringUtils.hasText(skuCode)) {
            termList.add(new Term("skuCode", skuCode));
        }

        String spuCode = params.get("spuCode"); // 货号
        if (StringUtils.hasText(spuCode)) {
            termList.add(new Term("spuCode", spuCode));
        }

        String itemType = params.get("type"); // 商品类型搜索
        if (StringUtils.hasText(itemType)) {
            termList.add(new Term("type", itemType));

            // 目前只有积分商品需要这样, 需要搜索纯积分商品
            String price = params.get("price"); // 商品价格搜索
            if (StringUtils.hasText(price)) {
                termList.add(new Term("price", price));
            }
        }
        return termList;
    }
}
