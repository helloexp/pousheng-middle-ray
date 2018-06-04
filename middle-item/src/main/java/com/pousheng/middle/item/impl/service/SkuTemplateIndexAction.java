/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.impl.service;

import com.pousheng.middle.item.SearchSkuTemplateProperties;
import com.pousheng.middle.item.dto.IndexedSkuTemplate;
import io.terminus.search.api.IndexTaskBuilder;
import io.terminus.search.api.model.IndexAction;
import io.terminus.search.api.model.IndexTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 索引操作 添加或删除
 * Author:  songrenfei
 * Date: 2017-11-19
 */
@Component
@Slf4j
public class SkuTemplateIndexAction {

    private final IndexTaskBuilder indexTaskBuilder;

    private final SearchSkuTemplateProperties searchItemProperties;

    @Autowired
    public SkuTemplateIndexAction(
                                  IndexTaskBuilder indexTaskBuilder,
                                  SearchSkuTemplateProperties searchItemProperties) {
        this.indexTaskBuilder = indexTaskBuilder;
        this.searchItemProperties = searchItemProperties;
    }

    public IndexTask indexTask(IndexedSkuTemplate indexedItem){
        return indexTaskBuilder.indexName(searchItemProperties.getIndexName())
                .indexType(searchItemProperties.getIndexType())
                .indexAction(IndexAction.INDEX).build(indexedItem.getId(), indexedItem);
    }

    public IndexTask deleteTask(Long skuTemplateId){
        return indexTaskBuilder.indexName(searchItemProperties.getIndexName())
                .indexType(searchItemProperties.getIndexType())
                .indexAction(IndexAction.DELETE).build(skuTemplateId, null);
    }
}
