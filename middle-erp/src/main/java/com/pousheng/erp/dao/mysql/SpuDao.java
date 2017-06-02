/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.erp.dao.mysql;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.spu.model.Spu;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * SPU dao
 * <p/>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-20
 */
@Repository
public class SpuDao extends MyBatisDao<Spu> {

    /**
     * 根据categoryId 查询spu列表
     *
     * @param categoryId 类目id
     * @return 该类目下的spu列表
     */
    public List<Spu> findByCategoryId(Long categoryId) {
        return getSqlSession().selectList(sqlId("findByCategoryId"), categoryId);
    }

    /**
     * 根据类目id及编码查询spu
     *
     * @param categoryId 类目id
     * @param spuCode spu编码
     * @return 该编码对应的spu
     */
    public Spu findByCategoryIdAndCode(Long categoryId, String spuCode) {
        return getSqlSession().selectOne(sqlId("findByCategoryIdAndSpuCode"),
                ImmutableMap.of("categoryId",categoryId, "spuCode",spuCode));
    }

    /**
     * 根据类目id和spu名称模糊匹配所有的spu列表
     *
     * @param categoryId 类目id
     * @param name       查询的名称
     * @return 符合条件的spu列表
     */
    public List<Spu> findByCategoryIdAndFuzzName(Long categoryId, String name) {
        return getSqlSession().selectList(sqlId("findByCategoryIdAndFuzzName"),
                ImmutableMap.of("categoryId", categoryId, "name", name));
    }

    /**
     * 根据类目id和spu名称精确匹配对应的spu, 这里假定同一类目下的spu不允许重名
     *
     * @param categoryId 类目id
     * @param name       spu名称
     * @return 符合条件的spu
     */
    public Spu findByCategoryIdAndName(Long categoryId, String name) {
        return getSqlSession().selectOne(sqlId("findByCategoryIdAndName"),
                ImmutableMap.of("categoryId", categoryId, "name", name));
    }

    /**
     * 根据spu id 更新对应的状态
     *
     * @param spuId  spu id
     * @param status 状态值
     */
    public void updateStatus(Long spuId, Integer status) {
        getSqlSession().update(sqlId("updateStatus"), ImmutableMap.of("id", spuId, "status", status));
    }

    /**
     * 更新spu 信息摘要
     *
     * @param spuId      spuId
     * @param spuInfoMd5 spu信息摘要
     */
    public void updateSpuInfoMd5(Long spuId, String spuInfoMd5) {
        getSqlSession().update(sqlId("updateSpuInfoMd5"),
                ImmutableMap.of("id", spuId, "spuInfoMd5", spuInfoMd5));
    }

    /**
     * 统计类目下有效spu的个数
     *
     * @param categoryId 类目id
     * @return 有效spu的个数
     */
    public Long countOfValidSpu(Long categoryId) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("categoryId", categoryId);
        final Long count = getSqlSession().selectOne(sqlId("count"), params);
        return MoreObjects.firstNonNull(count, 0L);
    }

    /**
     * 更新对应spu的extras及信息摘要快照
     *
     * @param spuId  spu id
     * @param extras  附加信息
     * @param spuInfoMd5  信息摘要快照
     */
    public void updateExtras(Long spuId, Map<String, String> extras, String spuInfoMd5) {
        getSqlSession().update(sqlId("updateExtras"),
                ImmutableMap.of("spuId", spuId,
                        "extraJson", JsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(extras),
                        "spuInfoMd5", spuInfoMd5));
    }
}
