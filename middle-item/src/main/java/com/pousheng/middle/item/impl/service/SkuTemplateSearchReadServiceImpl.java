/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.impl.service;

import com.google.common.collect.Maps;
import com.pousheng.middle.item.PsItemQueryBuilder;
import com.pousheng.middle.item.SearchSkuTemplateProperties;
import com.pousheng.middle.item.service.CriteriasWithShould;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.cache.CategoryBindingCacher;
import io.terminus.parana.cache.FrontCategoryCacher;
import io.terminus.parana.category.dto.FrontCategoryTree;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.search.api.Searcher;
import io.terminus.search.api.model.Pagination;
import io.terminus.search.api.model.WithAggregations;
import io.terminus.search.api.query.Criterias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 商品搜索服务
 * <p/>
 * Author:  songrenfei
 * Date: 2017-11-07
 */
@Service
@Slf4j
@RpcProvider
public class SkuTemplateSearchReadServiceImpl implements SkuTemplateSearchReadService {

    private static final String CAT_AGGS = "cat_aggs";
    private static final String ATTR_AGGS = "attr_aggs";
    private static final String BRAND_AGGS = "brand_aggs";

    private final SearchSkuTemplateProperties searchSkuTemplateProperties;

    private final PsItemQueryBuilder itemQueryBuilder;

    private final Searcher searcher;

    private final CategoryBindingCacher categoryBindingCacher;

    private final FrontCategoryCacher frontCategoryCacher;

    private final SkuTemplateSearchResultComposer skuTemplateSearchResultComposer;


    @Autowired
    public SkuTemplateSearchReadServiceImpl(SearchSkuTemplateProperties searchSkuTemplateProperties,
                                            PsItemQueryBuilder itemQueryBuilder,
                                            Searcher searcher,
                                            CategoryBindingCacher categoryBindingCacher,
                                            FrontCategoryCacher frontCategoryCacher,
                                            SkuTemplateSearchResultComposer skuTemplateSearchResultComposer) {
        this.searchSkuTemplateProperties = searchSkuTemplateProperties;
        this.itemQueryBuilder = itemQueryBuilder;
        this.searcher = searcher;
        this.categoryBindingCacher = categoryBindingCacher;
        this.frontCategoryCacher = frontCategoryCacher;
        this.skuTemplateSearchResultComposer = skuTemplateSearchResultComposer;
    }


    /**
     * 搜索商品, 并且包括属性导航, 面包屑等
     *
     * @param pageNo       起始页码
     * @param pageSize     每页记录条数
     * @param templateName 对应的搜索模板名称
     * @param params       搜索上下文
     * @return 搜索结果, 包括属性导航, 面包屑等
     */
    @Override
    public <T> Response<? extends SearchedItemWithAggs<T>> searchWithAggs(Integer pageNo, Integer pageSize,
                                                                          String templateName, Map<String, String> params,
                                                                          Class<T> clazz) {

        //对搜索参数进行预处理
        String brandIds = params.get("bids");
        final String aggSpecifiers = makeAggSpecifiers(brandIds);
        params.put("aggs", aggSpecifiers);

       // setHighlight(params);

        Map<String, Object> context = Maps.newHashMap();

        WithAggregations<T> withAggs = doSearch(pageNo, pageSize, templateName, params, clazz);
        SearchedItemWithAggs<T> searchedItemWithAggs = skuTemplateSearchResultComposer.compose(withAggs, params, context);
        return Response.ok(searchedItemWithAggs);
    }


    @Override
    public <T> Response<WithAggregations<T>> doSearchWithAggs(Integer pageNo, Integer pageSize,
                                                                          String templateName, Map<String, String> params,
                                                                          Class<T> clazz) {

        WithAggregations<T> withAggs = doSearch(pageNo, pageSize, templateName, params, clazz);
        return Response.ok(withAggs);
    }

    @Override
    public <T> Response<? extends Pagination<T>> searchWithScroll(String scrollId,Integer pageNo, Integer pageSize,
                                                                          String templateName, Map<String, String> params,
                                                                          Class<T> clazz) {
        Pagination<T> withAggs = doSearchWithScroll(scrollId,pageNo, pageSize, templateName, params, clazz);
        return Response.ok(withAggs);
    }


    private String makeAggSpecifiers(String brandId) {
        StringBuilder sb = new StringBuilder(ATTR_AGGS + ":attributes:"+Integer.MAX_VALUE);

        //如果指定了品牌, 则品牌不需要计算聚合了
        if (!StringUtils.hasText(brandId)) {
            sb.append("$" + BRAND_AGGS + ":brandId:"+Integer.MAX_VALUE);
        }
        //sb.append("$" + CAT_AGGS + ":categoryIds:"+Integer.MAX_VALUE);
        return sb.toString();
    }

    private void setHighlight(Map<String, String> params) {
        //如果关键字搜索不为空, 则设置高亮
        String q = params.get("q"); //搜索关键字
        if (StringUtils.hasText(q)) {
            params.put("highlight", "name");
        }
    }

    private <T> WithAggregations<T> doSearch(Integer pageNo, Integer pageSize,
                                             String templateName, Map<String, String> params,
                                             Class<T> clazz) {
        //构建搜索条件并进行搜索
        Criterias criterias = itemQueryBuilder.makeCriterias(pageNo, pageSize, params);
        return searcher.searchWithAggs(
                searchSkuTemplateProperties.getIndexName(),
                searchSkuTemplateProperties.getIndexType(),
                templateName,
                criterias,
                clazz);
    }

    private <T> Pagination<T> doSearchWithScroll(String scrollId,Integer pageNo, Integer pageSize,
                                                 String templateName, Map<String, String> params,
                                                 Class<T> clazz) {

        //构建搜索条件并进行搜索
        CriteriasWithShould criterias = itemQueryBuilder.makeCriterias(pageNo, pageSize, params);
        return searcher.searchWithScroll(
                searchSkuTemplateProperties.getIndexName(),
                searchSkuTemplateProperties.getIndexType(),
                templateName,
                criterias,
                clazz,Boolean.TRUE,scrollId,2);
    }

    /**
     * 获取指定前台类目树的所有叶子节点
     *
     * @param frontCategoryTree 前台类目树
     * @param leafFcids         叶子节点收集器
     */
    private void leafOf(FrontCategoryTree frontCategoryTree, List<Long> leafFcids) {

        //如果没有children了, 表明本节点就是叶子节点
        if (CollectionUtils.isEmpty(frontCategoryTree.getChildren())) {
            leafFcids.add(frontCategoryTree.getCurrent().getId());
        } else {
            //处理children, 递归收集
            for (FrontCategoryTree categoryTree : frontCategoryTree.getChildren()) {
                leafOf(categoryTree, leafFcids);
            }
        }
    }



}
