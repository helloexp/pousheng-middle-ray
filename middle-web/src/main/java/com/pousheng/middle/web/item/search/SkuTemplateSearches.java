/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.item.search;

import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.search.dto.SearchedItem;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Author: songrenfei
 * Date: 2017-11-22
 */
@RestController
@Slf4j
public class SkuTemplateSearches {

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    /**
     * 搜索商品, 并且包括属性导航, 面包屑等(主搜)
     *
     * @param pageNo       起始页码
     * @param pageSize     每页记录条数
     * @param params       搜索上下文
     * @return 搜索结果, 包括属性导航, 面包屑等
     */
    @RequestMapping(value = "/api/sku/template/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> searchItemWithAggs(@RequestParam(required = false) Integer pageNo,
                                                                                          @RequestParam(required = false) Integer pageSize,
                                                                                          @RequestParam Map<String,String> params){
        String templateName = "search.mustache";
        return skuTemplateSearchReadService.searchWithAggs(pageNo,pageSize, templateName, params, SearchSkuTemplate.class);
    }

}
