package com.pousheng.middle.group.impl.dao;

import com.pousheng.middle.group.model.ItemGroup;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Repository
public class ItemGroupDao extends MyBatisDao<ItemGroup> {

    public ItemGroup findByName(String name) {
        return getSqlSession().selectOne(sqlId("findByName"), name);
    }

    public List<ItemGroup> findAutoGroups() {
        return getSqlSession().selectList(sqlId("findAutoGroups"));
    }

    public List<ItemGroup> findByLikeName(String name) {
        return getSqlSession().selectList(sqlId("findByLikeName"),name);
    }

}
