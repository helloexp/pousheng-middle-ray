/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.item.search;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.PsSpuAttributeReadService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.search.dto.SearchedItem;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.SpuAttribute;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Author: songrenfei
 * Date: 2017-11-22
 */
@Api(description = "货品搜索API基于ES")
@RestController
@Slf4j
public class SkuTemplateSearches {

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private PsSpuAttributeReadService psSpuAttributeReadService;

    /**
     * 搜索商品, 并且包括属性导航, 面包屑等(主搜)
     *
     * @param pageNo       起始页码
     * @param pageSize     每页记录条数
     * @param params       搜索上下文 bid:品牌id、spuCode:货号、skuCode:货品条码、spuId:spu id、attrs: 属性查询（例如：年份:2017，季节:春）
     *                     bcids:后台类目id 、q:商品名称 、type: 1 mpos商品 0 非mpos商品
     * @return 搜索结果, 包括属性导航, 面包屑等
     */
    @ApiOperation("搜索货品")
    @RequestMapping(value = "/api/sku/template/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> searchItemWithAggs(@RequestParam(required = false) Integer pageNo,
                                                                                          @RequestParam(required = false) Integer pageSize,
                                                                                          @RequestParam Map<String,String> params){
        String templateName = "search.mustache";
        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response =skuTemplateSearchReadService.searchWithAggs(pageNo,pageSize, templateName, params, SearchSkuTemplate.class);
        if(response.isSuccess()){
            //封装信息
            assembleSkuInfo(response.getResult().getEntities().getData());
        }

        return response;
    }

    //封装价格 和 销售属性信息
    private void assembleSkuInfo(List<SearchSkuTemplate> data) {

        if(CollectionUtils.isEmpty(data)){
            return;
        }

        Map<Long,SkuTemplate> groupSkuTemplateById = groupSkuTemplateById(data);

        Map<Long, SpuAttribute> groupSpuAttributebySpuId = groupSpuAttributebySpuId(data);

        for (SearchSkuTemplate searchSkuTemplate : data){
            SkuTemplate skuTemplate = groupSkuTemplateById.get(searchSkuTemplate.getId());
            if (skuTemplate.getExtraPrice() != null) {
                searchSkuTemplate.setOriginPrice(skuTemplate.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY));
            }
            searchSkuTemplate.setPrice(skuTemplate.getPrice());
            Map<String,String> extra = skuTemplate.getExtra();
            if(!CollectionUtils.isEmpty(extra)&&extra.containsKey(PsItemConstants.MPOS_DISCOUNT)){
                searchSkuTemplate.setDiscount(Integer.valueOf(extra.get(PsItemConstants.MPOS_DISCOUNT)));
            }

            SpuAttribute spuAttribute = groupSpuAttributebySpuId.get(searchSkuTemplate.getSpuId());
            if(Arguments.notNull(spuAttribute)){
                searchSkuTemplate.setOtherAttrs(spuAttribute.getOtherAttrs());
            }
            searchSkuTemplate.setAttrs(skuTemplate.getAttrs());
            searchSkuTemplate.setMainImage(skuTemplate.getImage_());




        }




    }


    private Map<Long,SkuTemplate> groupSkuTemplateById(List<SearchSkuTemplate> data){
        List<Long> skuTemplateIds = Lists.transform(data, new Function<SearchSkuTemplate, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable SearchSkuTemplate input) {
                return input.getId();
            }
        });

        Response<List<SkuTemplate>>  skuTemplateRes = skuTemplateReadService.findByIds(skuTemplateIds);
        if(!skuTemplateRes.isSuccess()){
            log.error("find sku template by ids:{} fail,error:{}",skuTemplateIds,skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }


        return Maps.uniqueIndex(skuTemplateRes.getResult(), new Function<SkuTemplate, Long>() {
            @Override
            public Long apply(SkuTemplate skuTemplate) {
                return skuTemplate.getId();
            }
        });

    }

    private Map<Long, SpuAttribute> groupSpuAttributebySpuId(List<SearchSkuTemplate> data){

        List<Long> spuIds = Lists.transform(data, new Function<SearchSkuTemplate, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable SearchSkuTemplate input) {
                return input.getSpuId();
            }
        });

        Response<List<SpuAttribute>> spuAttributeRes = psSpuAttributeReadService.findBySpuIds(spuIds);
        if(!spuAttributeRes.isSuccess()){
            log.error("find sku spu attribute by spu ids:{} fail,error:{}",spuIds,spuAttributeRes.getError());
            throw new JsonResponseException(spuAttributeRes.getError());
        }

        return Maps.uniqueIndex(spuAttributeRes.getResult(), new Function<SpuAttribute, Long>() {
            @Override
            public Long apply(SpuAttribute spuAttribute) {
                return spuAttribute.getSpuId();
            }
        });
    }

}
