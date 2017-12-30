package com.pousheng.middle.item.impl.service;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.cache.BrandCacher;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.search.dto.*;
import io.terminus.parana.search.item.impl.BaseItemSearchResultComposer;
import io.terminus.parana.search.item.impl.ItemSearchResultComposer;
import io.terminus.search.api.model.WithAggregations;
import io.terminus.search.model.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author:cp
 * Created on 10/11/2016.
 */
@Service
@Slf4j
public class SkuTemplateSearchResultComposer extends BaseItemSearchResultComposer implements ItemSearchResultComposer<SearchedItemWithAggs> {

    private final BackCategoryCacher backCategoryCacher;

    private final BrandCacher brandCacher;

    public SkuTemplateSearchResultComposer(BackCategoryCacher backCategoryCacher,
                                           BrandCacher brandCacher) {
        this.backCategoryCacher = backCategoryCacher;
        this.brandCacher = brandCacher;
    }

    @Override
    public SearchedItemWithAggs compose(WithAggregations withAggs,
                                        Map<String, String> params,
                                        Map<String, Object> context) {

        SearchedItemWithAggs searchWithAggs = new SearchedItemWithAggs();

        //处理搜索结果
        Paging entities = new Paging<>(withAggs.getTotal(), withAggs.getData());
        searchWithAggs.setEntities(entities);

        //处理聚合
        Map<String, List<Bucket>> aggregations = withAggs.getAggregations();

        //处理类目聚合
        List<Bucket> catAggs = aggregations.get(CAT_AGGS);
        String chosenCats = params.get("bcids");

        Set<String> chosenCategories = Sets.newHashSet();
        if (StringUtils.hasText(chosenCats)) {
            for (String chosenCat : Splitters.UNDERSCORE.split(chosenCats)) {
                chosenCategories.add(chosenCat);
            }
        }

        List<AggNav> catNavs = makeCategoryNavs(catAggs, chosenCategories);
        searchWithAggs.setBackCategories(catNavs);

        //处理品牌聚合
        List<Bucket> brandAggs = aggregations.get(BRAND_AGGS);
        List<AggNav> brandNavs = makeBrandNavs(brandAggs);
        searchWithAggs.setBrands(brandNavs);

        //处理属性聚合
        //String chosenAttrs = params.get("attrs");
        String chosenAttrs = null; //skuTemplate搜索就不用已选择的功能
        List<Bucket> attrAggs = aggregations.get(ATTR_AGGS);
        List<GroupedAggNav> attrNavs = makeGroupedAttrNavs(attrAggs, chosenAttrs);
        searchWithAggs.setAttributes(attrNavs);

        //处理面包屑
        Long chosenCategoryId = 0L;
        if (chosenCategories.size() == 1) { //如果已选择某个后台类目, 则面包屑就用该类目
            chosenCategoryId = Long.valueOf(Iterables.get(chosenCategories, 0));
        } else if (catNavs != null && catNavs.size() == 1) { //如果结果只返回一个叶子类目, 则面包屑就用该叶子类目
            chosenCategoryId = (Long) catNavs.get(0).getKey();
        }
        List<IdAndName> breadCrumbs = makeBreadCrumbs(chosenCategoryId);
        searchWithAggs.setBreadCrumbs(breadCrumbs);

        //收集已选择的品牌和属性
        List<Chosen> chosens = Lists.newArrayList();

        //处理已选择的品牌
        addChosenBrands(params.get("bids"), chosens);

        //处理已选择的属性
        addChosenAttributes(chosenAttrs, chosens);

        //处理已选择的前台类目
        if (context.get("frontCategory") != null) {
            addChosenFrontCategory((FrontCategory) context.get("frontCategory"), chosens);
        }

        searchWithAggs.setChosen(chosens);

        return searchWithAggs;
    }

    private void addChosenFrontCategory(FrontCategory frontCategory, List<Chosen> chosens) {
        if (frontCategory != null) {
            chosens.add(new Chosen(3, frontCategory.getId(), frontCategory.getName()));
        }
    }

    private void addChosenBrands(String brandIds, List<Chosen> chosens) {
        if (StringUtils.hasText(brandIds)) {
            for (String brandId : Splitters.UNDERSCORE.split(brandIds)) {
                Brand brand = brandCacher.findBrandById(Long.valueOf(brandId));
                chosens.add(new Chosen(1, brandId, brand.getName()));
            }
        }
    }

    /**
     * 处理类目聚合, 移除掉已经选择的类目
     *
     * @param catAggs          类目聚合
     * @param chosenCategories 已选择的类目
     * @return 类目聚合
     */
    private List<AggNav> makeCategoryNavs(List<Bucket> catAggs, Set<String> chosenCategories) {

        if (CollectionUtils.isEmpty(catAggs)) {
            return null;
        }

        List<AggNav> catNavs = Lists.newArrayListWithCapacity(catAggs.size());
        for (Bucket catAgg : catAggs) {
            final String key = catAgg.getKey();
          /*  if (chosenCategories.contains(key)) { //忽略已选择的类目
                continue;
            }*/
            try {
                Long categoryId = Long.valueOf(key);
                BackCategory backCategory = backCategoryCacher.findBackCategoryById(categoryId);
                if (backCategory.getHasChildren()) {  //忽略非叶子类目
                    continue;
                }
                AggNav aggNav = new AggNav(categoryId, backCategory.getName(), catAgg.getDoc_count());
                catNavs.add(aggNav);
            } catch (Exception e) {
                log.error("failed to build cat navs for bucket(key={}),cause:{}",
                        key, Throwables.getStackTraceAsString(e));
            }
        }

        return catNavs;
    }


    private List<AggNav> makeBrandNavs(List<Bucket> brandAggs) {
        if (CollectionUtils.isEmpty(brandAggs)) {
            return null;
        }
        List<AggNav> brandNavs = Lists.newArrayListWithCapacity(brandAggs.size());
        for (Bucket brandAgg : brandAggs) {
            try {
                Long brandId = Long.valueOf(brandAgg.getKey());
                Brand brand = brandCacher.findBrandById(brandId);
                AggNav aggNav = new AggNav(brandId, brand.getName(), brandAgg.getDoc_count());

                // 暂时先在品牌额外信息中加入品牌 logo
                Map<String, String> extra = Collections.singletonMap("img", brand.getLogo_());
                aggNav.setExtra(extra);
                brandNavs.add(aggNav);
            } catch (Exception e) {
                log.error("failed to build brand navs for bucket(key={}),cause:{}",
                        brandAgg.getKey(), Throwables.getStackTraceAsString(e));
            }
        }
        return brandNavs;
    }

    /**
     * 找到当前分类到父分类的所有节点, 包括虚拟根节点0, 作为面包屑
     *
     * @param currentCategoryId 当前类目id
     * @return 面包屑
     */
    private List<IdAndName> makeBreadCrumbs(Long currentCategoryId) {
        List<IdAndName> idAndNames = Lists.newArrayList();
        while (currentCategoryId > 0) {
            BackCategory backCategory = backCategoryCacher.findBackCategoryById(currentCategoryId);
            idAndNames.add(new IdAndName(backCategory.getId(), backCategory.getName()));
            currentCategoryId = backCategory.getPid();
        }
        List<IdAndName> breadCrumbs = Lists.newArrayListWithCapacity(idAndNames.size() + 1);
        breadCrumbs.add(new IdAndName(0L, "所有分类"));
        breadCrumbs.addAll(Lists.reverse(idAndNames));
        return breadCrumbs;
    }


}
