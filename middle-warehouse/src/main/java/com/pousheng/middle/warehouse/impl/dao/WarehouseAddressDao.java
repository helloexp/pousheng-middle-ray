/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 菜鸟地址DAO
 *
 * Author  : panxin
 * Date    : 2:04 PM 3/5/16
 * Mail    : panxin@terminus.io
 */
@Repository
public class WarehouseAddressDao extends MyBatisDao<WarehouseAddress>{

    /**
     * 根据名称获取id
     *
     * @param name 地区名称
     * @return 地区
     */
    public WarehouseAddress findByName(String name) {
        return getSqlSession().selectOne(sqlId("findByName"), name);
    }

    /**
     * 查询pid下的地址列表
     *
     * @param pid 父级id
     * @return 地址列表
     */
    public List<WarehouseAddress> findByPid(Long pid) {
        return getSqlSession().selectList(sqlId("findByPid"), pid);
    }

    /**
     * 查询某级别的地区列表
     *
     * @param level 级别
     * @return 地区列表
     */
    public List<WarehouseAddress> findByLevel(Integer level) {
        return getSqlSession().selectList(sqlId("findByLevel"), level);
    }

    /**
     * 此新增的地址主键不采用自增策略，而是由调用者提供
     *
     * @param address 地址，包含期望的id值
     * @return 添加成功与否
     * @see MyBatisDao
     */
    public Boolean createWithId(WarehouseAddress address) {
        return getSqlSession().insert(sqlId("createWithId"), address) == 1;
    }
}
