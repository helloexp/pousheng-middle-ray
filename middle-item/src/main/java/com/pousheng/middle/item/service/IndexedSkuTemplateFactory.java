/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.service;

import com.pousheng.middle.item.dto.IndexedSkuTemplate;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.model.SpuAttribute;

/**
 * 创建dump到搜索引擎的商品对象, 包含属性, 类目等信息
 * Author: 宋仁飞
 * Date: 2017-11-31
 */
public interface IndexedSkuTemplateFactory {
    /**
     * 创建dump到搜索引擎的商品对象, 包含属性, 类目等信息
     *
     * @param skuTemplate          原有的商品对象
     * @param spu spu信息
     * @param spuAttribute 商品属性信息
     * @param others        可能还需要其他信息来组装
     * @return dump到搜索引擎的商品对象
     */
    IndexedSkuTemplate create(SkuTemplate skuTemplate, Spu spu, SpuAttribute spuAttribute, Object... others);
}
