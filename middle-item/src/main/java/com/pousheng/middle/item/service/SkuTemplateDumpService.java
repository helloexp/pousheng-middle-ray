/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.service;

import io.terminus.common.model.Response;

import java.util.List;

/**
 * dump服务
 * Author:  songrenfei
 * Date: 2017-11-15
 */
public interface SkuTemplateDumpService {
    /**
     * 全量dump商品
     */
    Response<Boolean> fullDump();

    /**
     * 增量dump商品
     * @param interval 间隔时间(分钟)
     */
    Response<Boolean> deltaDump(Integer interval);


    /**
     * 批量打标
     * @param skuTemplateIds skuTemplateId集合
     * @return 是否dump成功
     */
    Response<Boolean> batchDump(List<Long> skuTemplateIds);
}
