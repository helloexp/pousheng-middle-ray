/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.erp.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.brand.model.Brand;
import org.springframework.stereotype.Repository;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-15
 */
@Repository
public class BrandDao extends MyBatisDao<Brand> {

    /**
     * 根据名称查找品牌
     *
     * @param name 品牌名
     * @return 对应的品牌
     */
    public Brand findByName(String name) {
        return getSqlSession().selectOne(sqlId("findByName"), name);
    }

    /**
     * 根据外部编码查找品牌
     *
     * @param outerId 品牌名
     * @return 对应的品牌
     */
    public Brand findByOuterId(String outerId) {
        return getSqlSession().selectOne(sqlId("findByOuterId"), outerId);
    }
}
