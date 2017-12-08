/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.impl.service;

import com.pousheng.middle.item.SearchSkuTemplateProperties;
import io.terminus.parana.search.BaseESInitiator;
import io.terminus.search.core.ESClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * elasticsearch 的初始化准备工作, 需要创建商品索引和mapping
 * <p>
 * Author: songrenfei
 * Date: 2017-11-16
 */
@Component
@Slf4j
public class PsESItemInitiator extends BaseESInitiator {

    private final SearchSkuTemplateProperties searchSkuTemplateProperties;

    @Autowired
    public PsESItemInitiator(ESClient esClient, SearchSkuTemplateProperties searchSkuTemplateProperties) {
        super(esClient);
        this.searchSkuTemplateProperties = searchSkuTemplateProperties;
    }

    @PostConstruct
    public void init() throws Exception {
        init(searchSkuTemplateProperties.getIndexName(), searchSkuTemplateProperties.getIndexType(), searchSkuTemplateProperties.getMappingPath());
    }
}
