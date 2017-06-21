package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库Dao类
 * Date: 2017-06-21
 */
@Repository
public class WarehouseShopReturnDao extends MyBatisDao<WarehouseShopReturn> {

    public WarehouseShopReturn findByShopId(Long shopId) {
        return getSqlSession().selectOne(sqlId("findByShopId"), shopId);
    }
}
