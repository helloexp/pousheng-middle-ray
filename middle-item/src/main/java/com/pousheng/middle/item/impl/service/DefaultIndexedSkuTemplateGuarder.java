package com.pousheng.middle.item.impl.service;

import com.pousheng.middle.item.service.IndexedSkuTemplateGuarder;
import io.terminus.parana.spu.model.SkuTemplate;

/**
 * Author:songrenfei
 * Created on 13/12/2016.
 */
public class DefaultIndexedSkuTemplateGuarder implements IndexedSkuTemplateGuarder {
    @Override
    public boolean indexable(SkuTemplate skuTemplate) {
        return skuTemplate.getStatus() > 0;
    }
}
