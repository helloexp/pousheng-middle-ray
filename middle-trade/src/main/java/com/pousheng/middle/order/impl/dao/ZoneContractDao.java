package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.ZoneContract;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 区部联系人表Dao类
 * Date: 2018-04-04
 */
@Repository
public class ZoneContractDao extends MyBatisDao<ZoneContract> {

    public List<ZoneContract> findByZoneId(String zoneId) {
        ZoneContract contract = new ZoneContract();
        contract.setZoneId(zoneId);
        return getSqlSession().selectList(sqlId("findByCondition"), contract);
    }

}
