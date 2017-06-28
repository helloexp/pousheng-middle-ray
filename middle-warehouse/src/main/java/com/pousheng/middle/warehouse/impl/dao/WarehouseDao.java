package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库Dao类
 * Date: 2017-06-07
 */
@Repository
public class WarehouseDao extends MyBatisDao<Warehouse> {

    /**
     * 根据code做精确搜索
     *
     * @param code 仓库编码
     * @return 对应的仓库
     */
    public Warehouse findByCode(String code) {
        return getSqlSession().selectOne(sqlId("findByCode"), code);
    }

    /**
     * 根据code做模糊查询
     *
     * @param code 仓库编码
     * @return 符合条件的仓库列表
     */
    public List<Warehouse> findByFuzzyCode(String code){
        return getSqlSession().selectList(sqlId("findByFuzzyCode"), code);
    }
}
