package com.pousheng.middle.item.service;

import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.SkuTemplate;

import java.util.List;

/**
 * Created by songrenfei on 2017/12/7
 */
public interface PsSkuTemplateWriteService {
    /**
     * 更新SkuTemplate
     *
     * @param skuTemplate 待更新的sku template
     * @return 是否更新成功
     */
    Response<Boolean> update(SkuTemplate skuTemplate);


    /**
     * 批量更新图片
     * @param ids id集合
     * @param imageUrl 图片url
     * @return 是否成功
     */
    Response<Boolean> updateImageByIds(List<Long> ids, String imageUrl);


    /**
     * 批量更新类型
     * @param ids id集合
     * @param type 类型 {@link com.pousheng.middle.item.enums.PsSpuType}
     * @return 是否成功
     */
    Response<Boolean> updateTypeByIds(List<Long> ids, Integer type);


}
