/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.erp.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.spu.model.SkuTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模板商品sku dao
 * <p/>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-20
 */
@Repository
public class SkuTemplateDao extends MyBatisDao<SkuTemplate> {

    /**
     * 根据spuId查找对应的skuTemplate列表
     *
     * @param spuId spu id
     * @return 对应的skuTemplate列表
     */
    public List<SkuTemplate> findBySpuId(Long spuId) {
        return getSqlSession().selectList(sqlId("findBySpuId"), spuId);
    }

    /**
     * 根据skuCode查找对应的skuTemplate列表
     *
     * @param skuCode sku编码
     * @return 对应的skuTemplate列表
     */
    public List<SkuTemplate> findBySkuCode(String skuCode) {
        return getSqlSession().selectList(sqlId("findBySkuCode"),
                skuCode);
    }

    /**
     * 根据spuId和skuCode查找对应的skuTemplate
     *
     * @param spuId   spu id
     * @param skuCode sku 编码
     * @return 对应的SkuTemplate
     */
    public SkuTemplate findBySpuIdAndSkuCode(Long spuId, String skuCode) {
        return getSqlSession().selectOne(sqlId("findBySpuIdAndSkuCode"),
                ImmutableMap.of("spuId", spuId, "skuCode", skuCode));
    }

    /**
     * 根据spuId和skuCode更新对应的skuTemplate信息
     *
     * @param skuTemplate 待更新的 skuTemplate
     * @return 是否更新成功
     */
    public boolean updateBySpuIdAndSkuCode(SkuTemplate skuTemplate) {
        return getSqlSession().update(sqlId("updateBySpuIdAndSkuCode"), skuTemplate) == 1;
    }

    /**
     * 根据spu id更新sku template的状态
     *
     * @param spuId   spu id
     * @param status   状态
     */
    public void updateStatusBySpuId(Long spuId, Integer status) {
        getSqlSession().update(sqlId("updateStatusBySpuId"),
                ImmutableMap.of("spuId",spuId, "status",status));
    }

    public void deleteBySpuId(Long deletedSpuId) {
        getSqlSession().delete(sqlId("deleteBySpuId"), deletedSpuId);
    }
}
