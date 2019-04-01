/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "stocklog.search")
@Data
public class SearchStockLogProperties {

    /**
     * 库存变更日志的索引名称, 可能是alias
     */
    private String indexName ;

    /**
     * 库存变更日志的索引类型
     */
    private String indexType ;

    /**
     * 对应库存变更日志类型的索引文件路径, 用以初始化索引的mapping, 默认为 ${indexType}_mapping.json
     */
    private String mappingPath;

    /**
     * 全量dump索引时, 最多涉及多少天之前有更新的库存变更日志
     */
    private Integer fullDumpRange = 3;

    /**
     * 每次批量处理多少个库存变更日志
     */
    private Integer batchSize = 5000;
}
