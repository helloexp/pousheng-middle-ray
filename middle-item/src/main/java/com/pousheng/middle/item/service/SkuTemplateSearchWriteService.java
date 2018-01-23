/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.service;

import io.terminus.common.model.Response;

/**
 * 商品搜索谢服务
 *
 * Author:  songrenfei
 * Date: 2017-12-07
 */
public interface SkuTemplateSearchWriteService {

    /**
     * 索引商品
     *
     * @param skuTemplateId 商品id
     * @return  是否调用成功
     */
    Response<Boolean> index(Long skuTemplateId);

    /**
     * 删除商品
     *
     * @param skuTemplateId 商品id
     * @return  是否调用成功
     */
    Response<Boolean> delete(Long skuTemplateId);

    /**
     * 索引或者删除商品
     *
     * @param skuTemplateId  商品id
     * @return 是否调用成功
     */
    Response<Boolean> update(Long skuTemplateId);

}
