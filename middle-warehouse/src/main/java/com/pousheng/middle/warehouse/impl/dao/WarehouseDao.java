package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Author: jlchen
 * Desc: 仓库Dao类
 * Date: 2017-06-07
 */
@Repository
public class WarehouseDao extends MyBatisDao<Warehouse> {

    public Warehouse findByCode(String code) {
        return getSqlSession().selectOne(sqlId("findByCode"), code);
    }
}
