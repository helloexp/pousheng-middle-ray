package com.pousheng.middle.item.service;

import io.terminus.parana.spu.model.SkuTemplate;

/**
 * Author:songrenfei
 * Created on 13/12/2016.
 */
public interface IndexedSkuTemplateGuarder {

    /**
     * 检查商品是否可以dump到搜索引擎
     *
     * @param skuTemplate 商品信息
     * @return 可以返回true, 否则返回false
     */
    boolean indexable(SkuTemplate skuTemplate);

}
