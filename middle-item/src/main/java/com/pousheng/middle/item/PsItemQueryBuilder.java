/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.pousheng.middle.item.service.CriteriasWithShould;
import io.terminus.common.model.PageInfo;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.search.item.impl.DefaultItemQueryBuilder;
import io.terminus.search.api.query.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 搜索查询参数设置
 *
 * @author songrenfei
 */
@Slf4j
@Primary
@Component
public class PsItemQueryBuilder extends DefaultItemQueryBuilder {

    @Override
    public List<Term> buildTerm(Map<String, String> params) {
        List<Term> termList = Lists.newArrayList();
        // 搜索品牌
        String bid = params.get("bid");
        if (StringUtils.hasText(bid)) {
            termList.add(new Term("brandId", bid));
        }
        // 店铺内搜索
        String shopId = params.get("shopId");
        if (StringUtils.hasText(shopId)) {
            termList.add(new Term("shopId", shopId));
        }
        // 货品条码
        String skuCode = params.get("skuCode");
        if (StringUtils.hasText(skuCode)) {
            termList.add(new Term("skuCode", skuCode));
        }
        // 货品条码
        String spuId = params.get("spuId");
        if (StringUtils.hasText(spuId)) {
            termList.add(new Term("spuId", spuId));
        }
        // 货号
        String spuCode = params.get("spuCode");
        if (StringUtils.hasText(spuCode)) {
            termList.add(new Term("spuCode", spuCode));
        }

        // 分组
        String groupId = params.get("groupId");
        if (StringUtils.hasText(groupId)) {
            termList.add(new Term("groupIds", groupId));
        }
        // 排除分组
        String excludeGroupId = params.get("excludeGroupId");
        if (StringUtils.hasText(excludeGroupId)) {
            termList.add(new Term("excludeGroupIds", excludeGroupId));
        }
        // 商品类型搜索
        String itemType = params.get("type");
        if (StringUtils.hasText(itemType)) {
            termList.add(new Term("type", itemType));

            // 目前只有积分商品需要这样, 需要搜索纯积分商品
            // 商品价格搜索
            String price = params.get("price");
            if (StringUtils.hasText(price)) {
                termList.add(new Term("price", price));
            }
        }
        return termList;
    }

    @Override
    public CriteriasWithShould makeCriterias(Integer pageNo, Integer size, Map<String, String> params) {
        PsCriteriasBuilder criteriasBuilder = new PsCriteriasBuilder();
        PageInfo pageInfo = new PageInfo(pageNo, size);
        criteriasBuilder.withPageInfo(pageInfo.getOffset(), pageInfo.getLimit());
        Keyword keyword = this.buildKeyword(params);
        criteriasBuilder.withKeyword(keyword);
        List<Term> termList = this.buildTerm(params);
        criteriasBuilder.withTerm(termList);
        List<Terms> termsList = this.buildTerms(params);
        criteriasBuilder.withTerms(termsList);
        List<Range> ranges = this.buildRanges(params);
        criteriasBuilder.withRanges(ranges);
        List<Sort> sorts = this.buildSort(params);
        criteriasBuilder.withSorts(sorts);
        List<Aggs> aggsList = this.buildAggs(params);
        List<Highlight> highlightList = this.buildHighlight(params);
        criteriasBuilder.withHighlights(highlightList);
        criteriasBuilder.withAggs(aggsList);
        List<Terms> mustNotTerms = this.buildMustNotTerm(params);
        criteriasBuilder.withMustNotTerms(mustNotTerms);
        List<Terms> shouldNotTerms = this.buildShouldNotTerm(params);
        criteriasBuilder.withShouldNotTerms(shouldNotTerms);
        List<Terms> shouldTerms = this.buildShouldTerm(params);
        criteriasBuilder.withShouldTerms(shouldTerms);
        List<Range> shouldRanges = this.buildShouldRanges(params);
        Criterias criteria = criteriasBuilder.build();
        CriteriasWithShould criteriasWithShould = new CriteriasWithShould(criteriasBuilder);
        BeanUtils.copyProperties(criteria, criteriasWithShould);
        criteriasWithShould.shouldNotTerms(criteriasBuilder.getShouldNotTerms());
        criteriasWithShould.shouldTerms(criteriasBuilder.getShouldTerms());
        criteriasWithShould.shouldRanges(shouldRanges);
        return criteriasBuilder.build(criteriasWithShould);
    }

    private List<Terms> buildMustNotTerm(Map<String, String> params) {
        List<Terms> termList = Lists.newArrayList();
        String mustNotGroupId = params.get("mustNot_groupId");
        if (Objects.nonNull(mustNotGroupId)) {
            List<String> parts = Splitters.COMMA.splitToList(mustNotGroupId);
            termList.add(new Terms("groupIds", parts));
            termList.add(new Terms("excludeGroupIds", parts));
        }
        String mustNotBrandIds = params.get("mustNot_bids");
        if (Objects.nonNull(mustNotBrandIds)) {
            List<String> parts = Splitters.COMMA.splitToList(mustNotBrandIds);
            termList.add(new Terms("brandId", parts));
        }
        //处理属性 年份 季节
        String attributes = params.get("mustNot_attrs");
        analysisAttr(termList, attributes);
        //处理后台类目id列表 款型 系列 类别
        String bcids = params.get("mustNot_bcids");
        if (StringUtils.hasText(bcids)) {
            termList.add(new Terms("categoryIds", Splitters.UNDERSCORE.splitToList(bcids)));
        }
        return termList;
    }

    private List<Terms> buildShouldTerm(Map<String, String> params) {
        List<Terms> termList = Lists.newArrayList();
        String shouldNotBrandIds = params.get("should_bids");
        if (Objects.nonNull(shouldNotBrandIds)) {
            List<String> parts = Splitters.COMMA.splitToList(shouldNotBrandIds);
            termList.add(new Terms("brandId", parts));
        }
        //处理属性 年份 季节
        String attributes = params.get("should_attrs");
        analysisAttr(termList, attributes);
        //处理后台类目id列表 款型 系列 类别
        String bcids = params.get("should_bcids");
        if (StringUtils.hasText(bcids)) {
            termList.add(new Terms("categoryIds", Splitters.UNDERSCORE.splitToList(bcids)));
        }
        return termList;
    }


    private List<Terms> buildShouldNotTerm(Map<String, String> params) {
        List<Terms> termList = Lists.newArrayList();
        String shouldNotBrandIds = params.get("shouldNot_bids");
        if (Objects.nonNull(shouldNotBrandIds)) {
            List<String> parts = Splitters.COMMA.splitToList(shouldNotBrandIds);
            termList.add(new Terms("brandId", parts));
        }
        //处理属性 年份 季节
        String attributes = params.get("shouldNot_attrs");
        analysisAttr(termList, attributes);
        //处理后台类目id列表 款型 系列 类别
        String bcids = params.get("shouldNot_bcids");
        if (StringUtils.hasText(bcids)) {
            termList.add(new Terms("categoryIds", Splitters.UNDERSCORE.splitToList(bcids)));
        }
        return termList;
    }

    @Override
    public List<Terms> buildTerms(Map<String, String> params) {
        List<Terms> termsList = Lists.newArrayList();
        //处理属性
        String attributes = params.get("attrs");
        analysisAttr(termsList, attributes);

        String bids = params.get("bids");
        //处理品牌
        if (StringUtils.hasText(bids)) {
            final List<String> parts = Splitters.COMMA.splitToList(bids);
            termsList.add(new Terms("brandId", parts));
        }
        // 处理区域id
        String regionIds = params.get("regionIds");
        if (StringUtils.hasText(regionIds)) {
            termsList.add(new Terms("regionIds", Splitters.UNDERSCORE.splitToList(regionIds)));
        }
        //处理id列表
        String ids = params.get("ids");
        if (StringUtils.hasText(ids)) {
            termsList.add(new Terms("id", Splitters.UNDERSCORE.splitToList(ids)));
        }
        //处理后台类目id列表
        String bcids = params.get("bcids");
        if (StringUtils.hasText(bcids)) {
            termsList.add(new Terms("categoryIds", Splitters.UNDERSCORE.splitToList(bcids)));
        }
        // 分组
        String groupIds = params.get("groupIds");
        if (StringUtils.hasText(groupIds)) {
            termsList.add(new Terms("groupIds", Splitters.COMMA.splitToList(groupIds)));
        }
        // 货号
        String materialIds = params.get("materialIds");
        if (StringUtils.hasText(materialIds)) {
            termsList.add(new Terms("spuCode", Splitters.COMMA.splitToList(materialIds)));
        }
        // 商品编码
        String skuCodes = params.get("skuCodes");
        if (StringUtils.hasText(skuCodes)) {
            termsList.add(new Terms("skuCode", Splitters.COMMA.splitToList(skuCodes)));
        }
        return termsList;
    }

    @Override
    public List<Range> buildRanges(Map<String, String> params) {
        List<Range> ranges = Lists.newArrayList();
        String after = params.get("after");
        String before = params.get("before");
        DateTime today= new DateTime().dayOfWeek().roundFloorCopy();
        if (StringUtils.hasText(after) || StringUtils.hasText(before)) {
            Range dateRange = new Range("saleDate", StringUtils.isEmpty(before) ? null
                    : today.minusDays(Integer.parseInt(before)).toDate().getTime(),
                    StringUtils.isEmpty(after) ? null
                            : today.minusDays(Integer.parseInt(after)).toDate().getTime());
            ranges.add(dateRange);
        }
        return ranges;
    }


    public List<Range> buildShouldRanges(Map<String, String> params) {
        List<Range> ranges = Lists.newArrayList();
        String after = params.get("should_after");
        String before = params.get("should_before");
        DateTime today= new DateTime().dayOfWeek().roundFloorCopy();
        if (StringUtils.hasText(before)) {
            Range dateRange = new Range("saleDate", today.minusDays(Integer.parseInt(before) - 1).toDate().getTime(),
                    null);
            ranges.add(dateRange);
        }
        if (StringUtils.hasText(after)) {
            Range dateRange = new Range("saleDate", null, today.minusDays(Integer.parseInt(after) + 1).toDate().getTime());
            ranges.add(dateRange);
        }
        return ranges;
    }


    private void analysisAttr(List<Terms> termsList, String attributes) {
        if (StringUtils.hasText(attributes)) {
            final List<String> parts = Splitters.UNDERSCORE.splitToList(attributes);
            ArrayListMultimap<String, String> groupByKey = ArrayListMultimap.create();
            for (String part : parts) {
                List<String> kvs = Splitters.COMMA.splitToList(part);
                for (String kvPart : kvs) {
                    List<String> kv = Splitters.COLON.splitToList(kvPart);
                    if (kv.size() != 2) {
                        log.warn("kv not split by `:', kv={}", kv);
                        continue;
                    }
                    groupByKey.put(kv.get(0), kvPart);
                }
            }
            for (String key : groupByKey.keySet()) {
                List<String> kvs = groupByKey.get(key);
                if (!kvs.isEmpty()) {
                    termsList.add(new Terms("attributes", new ArrayList<>(kvs)));
                }
            }
        }
    }

}
