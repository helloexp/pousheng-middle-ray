/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.erp.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.spu.model.SpuDetail;
import org.springframework.stereotype.Repository;

/**
 * SPU 详情dao
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-20
 */
@Repository
public class SpuDetailDao extends MyBatisDao<SpuDetail> {
    /**
     * 根据商品id查询商品详情
     * @param spuId 商品编号
     * @return 商品详情
     */
    public SpuDetail findBySpuId(Long spuId){
        return getSqlSession().selectOne(sqlId("findBySpuId"), spuId);
    }

    /**
     * 删除商品的详情
     * @param spuId 商品id
     * @return 删除记录数
     */
    public Integer deleteBySpuId(Long spuId) {
        return getSqlSession().delete(sqlId("delete"), spuId);
    }
}
