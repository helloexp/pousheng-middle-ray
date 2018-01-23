/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 搜索配置参数
 * Author:  songrenfei
 * Date: 2017-11-13
 */
@ConfigurationProperties(prefix = "skutemplate.search")
@Getter
@Setter
public class SearchSkuTemplateProperties {

    /**
     * 商品的索引名称, 可能是alias
     */
    private String indexName = "skuTemplates";

    /**
     * 商品的索引类型
     */
    private String indexType = "skuTemplate";

    /**
     * 对应商品类型的索引文件路径, 用以初始化索引的mapping, 默认为 ${indexType}_mapping.json
     */
    private String mappingPath;

    /**
     * 全量dump索引时, 最多涉及多少天之前有更新的商品
     */
    private Integer fullDumpRange = 3;

    /**
     * 每次批量处理多少个商品
     */
    private Integer batchSize = 100;
}
