/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.erp.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.category.model.BackCategory;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-15
 */
@Repository
public class BackCategoryDao extends MyBatisDao<BackCategory> {

    /**
     * 根据父类目id查找对应子类目列表
     *
     * @param pid 父类目id
     * @return 对应的子类目列表
     */
    public List<BackCategory> findChildren(Long pid) {
        return getSqlSession().selectList(sqlId("findByPid"), pid);
    }

    /**
     * 根据父类目id及类目名称查找对应的子类目
     *
     * @param pid  父类目id
     * @param name  子类目名称
     * @return   符合条件的子类目
     */
    public BackCategory findChildrenByName(Long pid, String name) {
        return getSqlSession().selectOne(sqlId("findByPidAndName"),
                ImmutableMap.of("pid", pid, "name", name));
    }
}
