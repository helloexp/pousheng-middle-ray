/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.service;

import io.terminus.common.model.Response;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.search.api.model.Pagination;
import io.terminus.search.api.model.WithAggregations;
import io.terminus.common.model.Paging;
import java.util.Map;

/**
 * 商品搜索读服务
 * <p>
 * Author:  songrenfei
 * Date: 2017-11-07
 */
public interface SkuTemplateSearchReadService {

    /**
     * 搜索商品, 并且包括属性导航, 面包屑等
     *
     * @param pageNo       起始页码
     * @param pageSize     每页记录条数
     * @param templateName 对应的搜索模板名称
     * @param params       搜索上下文
     * @param clazz        搜索结果类型
     * @return 搜索结果, 包括属性导航, 面包屑等
     */
    <T> Response<? extends SearchedItemWithAggs<T>> searchWithAggs(Integer pageNo, Integer pageSize,
                                                                   String templateName, Map<String, String> params,
                                                                   Class<T> clazz);

    /**
     * 仅搜索商品, 不再聚合搜索结果
     *
     * @param pageNo       起始页码
     * @param pageSize     每页记录条数
     * @param templateName 对应的搜索模板名称
     * @param params       搜索上下文
     * @param clazz        搜索结果类型
     * @return 搜索结果, 包括属性导航, 面包屑等
     */
    <T> Response<WithAggregations<T>> doSearchWithAggs(Integer pageNo, Integer pageSize,
                                                              String templateName, Map<String, String> params,
                                                              Class<T> clazz);

    /**
     * 深度分页搜索商品
     *
     * @param scrollId     scrollId
     * @param pageNo       起始页码
     * @param pageSize     每页记录条数
     * @param templateName 对应的搜索模板名称
     * @param params       搜索上下文
     * @param clazz        搜索结果类型
     * @return 搜索结果, 包括属性导航, 面包屑等
     */
    <T> Response<? extends Pagination<T>> searchWithScroll(String scrollId,Integer pageNo, Integer pageSize,
                                                         String templateName, Map<String, String> params,
                                                         Class<T> clazz);

    /**
     * 根據料號過濾資料
     * @param params
     * @return
     */
    Response<Paging<SkuTemplate>> findByMaterial(Integer offset, Integer limit, Map<String, Object> params);

}

