/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.erp.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.spu.model.SpuAttribute;
import org.springframework.stereotype.Repository;

/**
 * SPU 相关属性DAO
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-20
 */
@Repository
public class SpuAttributeDao extends MyBatisDao<SpuAttribute> {

    /**
     * 根据spu id查找spu属性
     *
     * @param spuId spu id
     * @return  对应的spu属性
     */
    public SpuAttribute findBySpuId(Long spuId){
        return getSqlSession().selectOne(sqlId("findBySpuId"), spuId);
    }

    /**
     * 更新spu属性
     *
     * @param spuAttribute   spu属性
     * @return   是否更新成功
     */
    public boolean updateBySpuId(SpuAttribute spuAttribute){
        return getSqlSession().update(sqlId("update"), spuAttribute) == 1;
    }

    /**
     * 删除spu属性
     *
     * @param spuId   spu属性
     * @return    是否删除成功
     */
    public boolean deleteBySpuId(Long spuId){
        return getSqlSession().delete(sqlId("delete"),spuId) == 1;
    }

}
