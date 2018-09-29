package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.VipWarehouseMapping;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: zhaoxiaowei
 * Desc: Dao类
 * Date: 2018-09-29
 */
@Repository
public class VipWarehouseMappingDao extends MyBatisDao<VipWarehouseMapping> {


    public VipWarehouseMapping findByWarehouseId(Long warehouseId) {
        return getSqlSession().selectOne(sqlId("findByWarehouseId"), warehouseId);
    }

    public List<Long> findAllWarehouseIds() {
        return getSqlSession().selectList(sqlId("findAllWarehouseIds"));
    }

    public Boolean deleteByWarehouseId(Long warehouseId) {
        return getSqlSession().delete(sqlId("deleteByWarehouseId"), warehouseId) == 1;
    }
}
