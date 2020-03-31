/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.service;

import io.terminus.common.model.Response;
import io.terminus.search.api.model.WithAggregations;

import java.util.Map;

/**
 * 商品搜索读服务
 * <p>
 * Author:  songrenfei
 * Date: 2017-11-07
 */
public interface StockLogSearchReadService {

    /**
     * 搜索日志
     *
     * @param pageNo       起始页码
     * @param pageSize     每页记录条数
     * @param templateName 对应的搜索模板名称
     * @param params       搜索上下文
     * @param clazz        搜索结果类型
     * @return 搜索结果, 包括属性导航, 面包屑等
     */
    <T> Response<? extends WithAggregations<T>> searchWithAggs(Integer pageNo, Integer pageSize,
                                                               String templateName, Map<String, String> params,
                                                               Class<T> clazz);


}

